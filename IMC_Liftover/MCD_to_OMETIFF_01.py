# -*- coding: utf-8 -*-
"""
Created on Tue Apr 26 12:07:55 2022

@author: m088378
"""
import numpy as np
import pandas as pd
import xarray as xr
import xmltodict
import tifffile
import re, os, sys, glob
from pathlib import Path
from typing import Union, List, Sequence, Generator
import mmap
import struct
from pprint import pprint

class ROIData:
	"""Represents image data (individual ROI) in long form. Meant to be instantiated by
	file readers and convert long-form image data into data arrays behind the scenes."""
	def __init__(self, df, name, attrs=None):
		"""df is a long-form dataarray with columns zero-indexed X, Y as a MultiIndex with
		additional columns as channel intensities."""
		self.df = df
		self.name = name
		self.attrs = attrs

	@classmethod
	def from_txt(cls, path):
		"""Initialize image from .txt file."""
		# First pass to validate text file columns are consistent with IMC data
		header_cols = pd.read_csv(path, sep="\t", nrows=0).columns
		expected_cols = ("Start_push", "End_push", "Pushes_duration", "X", "Y", "Z")
		if tuple(header_cols[:6]) != expected_cols or len(header_cols) <= 6:
			raise ValueError(
				f"'{str(path)}' is not valid IMC text data (expected first 6 columns: {expected_cols}, plus intensity data)."
			)
		# Actual read, dropping irrelevant columns and casting image data to float32
		txt = pd.read_csv(
			path, sep="\t",
			usecols=lambda c: c not in ("Start_push", "End_push", "Pushes_duration", "Z"),
			index_col=["X", "Y"],
			dtype={c: np.float32 for c in header_cols[6:]}
		)
		# Rename columns to be consistent with .mcd format
		txt.columns = [_parse_txt_channel(col) for col in txt.columns]
		return cls(txt, Path(path).stem)

	def _df_to_array(self):
		xsz, ysz = self.df.index.to_frame().max()[["X", "Y"]] + 1
		pprint(['_df_to_array','xsz',xsz,'ysz',ysz])
		csz = len(self.df.columns)
		# Ensure X/Y are fully specified, and fill in missing indices if needed
		multiindex = pd.MultiIndex.from_product([range(xsz), range(ysz)],
												 names=["X", "Y"])
		# This will create nan rows if certain x/y combinations are missing:
		df_fill = self.df.reindex(multiindex)
		# Sort values by ascending Y, then X, so that we can reshape in C index order
		return df_fill.sort_values(["Y", "X"]).values.reshape((ysz, xsz, csz))

	def as_dataarray(self, fill_missing):
		# Reshape long-form data to image
		arr = self._df_to_array()
		# Try to fill missing values if necessary
		nan_mask = np.isnan(arr)
		if fill_missing is None and nan_mask.sum() > 0:
			raise ValueError("Image data is missing values. Try specifying 'fill_missing'.")
		arr[np.isnan(arr)] = fill_missing
		return xr.DataArray(arr,
			name=self.name,
			dims=("y", "x", "c"),
			coords={"x": range(arr.shape[1]), "y": range(arr.shape[0]),
					"c": self.df.columns.tolist()},
			attrs=self.attrs
		)


def _parse_txt_channel(header: str) -> str:
	"""Extract channel and label from text headers and return channels as formatted by
	MCDViewer. e.g. 80ArAr(ArAr80Di) -> ArAr(80)_80ArAr
	Args:
		headers: channel text header
	Returns:
		Channel header renamed to be consistent with MCD Viewer output
	"""
	label, metal, mass = re.findall(r"(.+)\(([a-zA-Z]+)(\d+)Di\)", header)[0]
	return f"{metal}({mass})_{label}"


def read_txt(path: Union[Path, str], fill_missing: float=-1) -> xr.DataArray:
	"""Read a Fluidigm IMC .txt file and returns the image data as an xarray DataArray.
	This is a convenience function which avoids instantiating ROIData.
	Args:
		path: path to IMC .txt file.
		fill_missing: value to use to fill in missing image data. If not specified,
			an error will be raised if there is missing image data.
	Returns:
		An xarray DataArray containing multichannel image data.
	Raises:
		ValueError: File is not valid IMC text data or missing values."""
	return ROIData.from_txt(path).as_dataarray(fill_missing)


def _parse_mcd_channel(attr: dict) -> str:
	"""Extract channel and label from MCD channel XML data."""
	if attr["ChannelName"] in ("X", "Y", "Z"):
		return attr["ChannelName"]
	if attr["ChannelLabel"] is None:  # Use ChannelName to create label in right format
		label, mass = re.findall(r"(.+)\((\d+)\)", attr["ChannelName"])[0]
		attr["ChannelLabel"] = f"{label}{mass}"
	return f"{attr['ChannelName']}_{attr['ChannelLabel']}"


def read_mcd(path: Union[Path, str], fill_missing: float=-1, encoding: str="utf-16-le"
			) -> Generator[xr.DataArray, None, None]:
	"""Read a Fluidigm IMC .mcd file and yields xarray DataArray
	(since MCD files can contain more than one image).
	Args:
		path: path to IMC .mcd file.
		fill_missing: value to use to fill in missing image data. If not specified,
			an error will be raised if there is missing image data.
		encoding: specifies the Unicode encoding of the XML section (defaults to UTF-16-LE).
	Returns:
		A generator of xarray DataArrays containing multichannel image data.
	"""
	with open(path, mode="rb") as fh:
		with mmap.mmap(fh.fileno(), 0, access=mmap.ACCESS_READ) as mm:
			# MCD format documentation recommends searching from end for "<MCDPublic"
			offset = mm.rfind("<MCDPublic".encode(encoding))
			if offset == -1:
				raise ValueError(f"'{str(path)}' does not contain MCDPublic XML footer (try different encoding?).")
			mm.seek(offset)
			xml = mm.read().decode(encoding)
			# Parse xml, force Acquisition(Channel) to be list even if single item so we can
			# always iterate over it
			root = xmltodict.parse(xml,
				force_list=("Acquisition", "AcquisitionChannel"))["MCDPublic"]
			acquisitions = root["Acquisition"]
			for acq in acquisitions:
				id_ = acq["ID"]
				channels = sorted(
					[ch for ch in root["AcquisitionChannel"] if ch["AcquisitionID"] == id_],
					key=lambda c: int(c["OrderNumber"])
				)
				channel_names = [_parse_mcd_channel(dict(c)) for c in channels]
				# Parse 4-byte float values
				# Data consists of values ordered X, Y, Z, C1, C2, ..., CN (and so on)
				if acq["SegmentDataFormat"] != "Float" or acq["ValueBytes"] != "4":
					raise NotImplementedError("Expected float32 data in 'SegmentDataFormat' tag.")
				mm.seek(int(acq["DataStartOffset"]))
				raw = mm.read(int(acq["DataEndOffset"]) - int(acq["DataStartOffset"]))
				arr = np.array(
					[struct.unpack("f", raw[i:i+4])[0] for i in range(0, len(raw), 4)],
					dtype=np.float32
				).reshape((-1, len(channels)))
				df = (pd.DataFrame(arr, columns=channel_names)
					.drop(columns=["Z"])
					.astype({"X": np.int64, "Y": np.int64})
					.set_index(["X", "Y"]))
				if df.shape[0] < 9:
					print("WARNING: Skipping empty data!")
					pprint(['df',df.shape])
					pprint(['acquisitions',acq])
					continue
				yield ROIData(df, f"{Path(path).stem}_{id_}", acq).as_dataarray(fill_missing)

### Custom format modifiers, based on Lab-tech errors.
def getCustomModifications(w):
	w = w.replace('145NdCD5', '145Nd-CD5')
	w = w.replace('146NdCD16', '146Nd-CD16')
	return w

def markerlist_to_dict(lst):
	markers = {}
	for m in lst:
		markers[m] = {}

		mMod = getCustomModifications(m)
		arr = mMod.split("-", 1)
		if len(arr) == 2:
			if arr[1] == "BKG":
				markers[m]['Mark'] = '-'
			else:
				markers[m]['Mark'] = arr[1].replace(" ","-")
		else:
			markers[m]['Mark'] = '-'

		arr2 = arr[0].split("_")
		arr2[1] = re.sub('^cd', 'CD', arr2[1])
		markers[m]['Ion'] = arr2[0]
		markers[m]['Name'] = arr2[1]
	return markers

# which I set as either 1 or 1.54.(Assume IMC is 1um/pixel)
def write_ometiff(imarr: xr.DataArray, outpath: Union[Path, str], **kwargs) -> None:
	"""Write DataArray to a multi-page OME-TIFF file.
	Args:
		imarr: image DataArray object
		outpath: file to output to
		**kwargs: Additional arguments to tifffile.imwrite
	"""
	outpath = Path(outpath)
	imarr = imarr.transpose("c", "y", "x")

	### Remove Select Channels that we know are not part of deliveryable
	panelDetails = markerlist_to_dict( list(imarr.c.values) )
	#print("List of Markers")
	#pprint(panelDetails)

	toDropList = list({ key:value for (key,value) in panelDetails.items() if value['Mark'] == '-'}.keys())
	pprint(toDropList)


	imarr = imarr.drop_sel(c=toDropList)


	Nc, Ny, Nx = imarr.shape
	# Generate standard OME-XML
	channels_xml = '\n'.join(
		[f"""<Channel ID="Channel:0:{i}" Name="{panelDetails[channel]['Mark']}" Fluor="{panelDetails[channel]['Ion']}" SamplesPerPixel="1"  ContrastMethod="Fluorescence" />"""
			for i, channel in enumerate(imarr.c.values)]
	)
	xml = f"""<?xml version="1.0" encoding="UTF-8"?>
	<OME xmlns="http://www.openmicroscopy.org/Schemas/OME/2016-06"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://www.openmicroscopy.org/Schemas/OME/2016-06 http://www.openmicroscopy.org/Schemas/OME/2016-06/ome.xsd">
		<Image ID="Image:0" Name="{outpath.stem}">
			<Pixels BigEndian="false"
					DimensionOrder="XYZCT"
					ID="Pixels:0"
					Interleaved="false"
					SizeC="{Nc}"
					SizeT="1"
					SizeX="{Nx}"
					SizeY="{Ny}"
					SizeZ="1"
					PhysicalSizeX="1.0"
					PhysicalSizeY="1.0"
					Type="float">
				<TiffData />
				{channels_xml}
			</Pixels>
		</Image>
	</OME>
	"""
	outpath.parent.mkdir(parents=True, exist_ok=True)
	# Note resolution: 1 um/px = 25400 px/inch
	tifffile.imwrite(outpath, data=imarr.values, description=xml, contiguous=True, resolution=(25400, 25400, "inch"))


#Skip Xe channel.
##Skip empty images.
#Remove BKG channels.
#Remove Pt channels. (give full list of specturm, which channels will never on channel)
#outDir = Path(r'O:\PUBLIC\Hyperion_Imaging\Comfere_Nneke_MD\RaymondDevelopments\OMETIFFs')
#outDir = Path(r'R:\PUBLIC\2022\04_April\Alexander_Req32321\OMETIFFs')
#outDir = Path(r'R:\PUBLIC\2022\02_February\hyperion_AF\OMETIFFs')
#outDir = Path(r'R:\PUBLIC\2021\09_September\Hyperion_Bartemes\OMETIFFs')
#base=r'O:\PUBLIC\Hyperion_Imaging\Comfere_Nneke_MD\Hyperion_mcd_files'
#base=r'R:\PUBLIC\2022\04_April\Alexander_Req32321\Alexander_MCD_Files'
#base=r'R:\PUBLIC\2022\02_February\hyperion_AF'
#base=r'R:\PUBLIC\2021\09_September\Hyperion_Bartemes\MCD\Control_Tonsil'
#outDir = Path(r'R:\PUBLIC\2022\03_March\Hyperion_MG\OMETIFFs')
#base=r'R:\PUBLIC\2022\03_March\Hyperion_MG\req31898March2022\FrozenTissue'

#outDir = Path(r'O:\2022\09_September\20220906_KB_Hyperion\OMETIFFs')
#base=r'O:\2022\09_September\20220906_KB_Hyperion'
outDir = Path(r'R:\PUBLIC\2021\09_September\Hyperion_Bartemes\OMETIFFs')
base=r'R:\PUBLIC\2021\09_September\Hyperion_Bartemes\MCD_Files'

mcdFiles = [Path(ff) for ff in list(filter(os.path.isfile, glob.glob( os.path.join(base,'*.mcd')) ))]
#mcdFiles = [Path(ff) for ff in list(filter(os.path.isfile, glob.glob( os.path.join(base,"*",'*.mcd')) ))]

#mcdFiles = [m for m in mcdFiles if not m.name.startswith("REQ")]

#pprint(mcdFiles)
for f in mcdFiles:
	arrs = read_mcd(f)
	for arr in arrs:
		outname = f"{f.stem}_{arr.Description}" if f.suffix == ".mcd" else f.stem
		#pprint(arr)
		print(outname)
		write_ometiff(arr, outDir / f"{outname}.ome.tiff"  )