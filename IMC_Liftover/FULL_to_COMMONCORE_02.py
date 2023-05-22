# -*- coding: utf-8 -*-
"""
Created on Tue Apr 26 12:54:39 2022

@author: m088378
"""
import numpy as np
import pandas as pd
import xarray as xr
import tifffile as tf
import xml.etree.ElementTree as ET
import xmltodict
import re, os, sys, glob
from pathlib import Path
from typing import Union, List, Sequence, Generator
import mmap
import struct
from pprint import pprint

### Common Core list at Spring 2022
commonCoreList = [ 'SMA', 'CD19', 'VIMENTIN', 'CD14', 'CD16', 'PANCK', 'CD11B', 'CD45', 'CD11C', 'FOXP3',
				 'CD4', 'ECADHERIN', 'CD68', 'VISTA', 'CD20', 'CD8A', 'CD45RA', 'GRANZYMEB', 'KI67', 'COLLAGEN',
				 'CD3', 'HISTONEH3', 'CD45RO', 'HLADR', 'BETA2M', 'NAKATPASE', 'DNA']

### Common Core list to include LaRusso June 2020
#commonCoreList = [ 'SMA', 'CD19', 'VIMENTIN', 'CD14', 'CD16', 'PANCK', 'CD11B', 'CD45', 'CD11C', 'FOXP3',
#				 'CD4', 'ECADHERIN', 'CD68', 'VISTA', 'CD20', 'CD8A', 'CD45RA', 'GRANZYMEB', 'COLLAGEN',
#				 'CD3', 'CD45RO', 'HLADR', 'DNA']

### Common Core list to include Sumera Sept 2022
#commonCoreList = [ 'ASMA', 'CASP3', 'CD11B', 'CD14', 'CD141', 'CD15', 'CD16', 'CD163', 'CD169', 'CD19', 'CD20', 
# 'CD206', 'CD27', 'CD3', 'CD38', 'CD4', 'CD45', 'CD68', 'CD8', 'CK19', 'DAPI', 'FOXP3', 'GRAN_B', 'HLA_I', 'HLA_II', 
# 'KAPPA', 'KI67', 'LAMBDA', 'MPO', 'NAKATPASE' ]


base=r'R:\PUBLIC\2021\09_September\Hyperion_Bartemes\OMETIFFs'
outdir=r'R:\PUBLIC\2021\09_September\Hyperion_Bartemes\COMMONCORE'
#base=r'R:\PUBLIC\2022\02_February\hyperion_AF\OMETIFFs'
#outdir=r'R:\PUBLIC\2022\02_February\hyperion_AF\COMMONCORE'
#base=r'R:\PUBLIC\2020\06_June\LaRusso_Project_02\OMETIFFs'
#outdir=r'R:\PUBLIC\2020\06_June\LaRusso_Project_02\COMMONCORE'
inFiles =  list(filter(os.path.isfile, glob.glob( os.path.join(base, '*.ome.tiff')) ))
inFiles = [m for m in inFiles if not "REQ" in m]

def formatMetadata(tiffHeader):
	idx = 0
	acquisitions=[]
	for ele in tiffHeader.findall('.//*'):
		chMeta = dict(ele.attrib)
		#print("Index: {}".format(idx))
		if 'Fluor' in chMeta:
			lbl = ele.attrib['Fluor']
			opLabel = ele.attrib['Name'].upper().replace('-','').replace('_','')
			## Custom Label Transformations to account for operator indiscretion. 
			if opLabel == "NA1":
				opLabel = "DNA"
			if opLabel == "PANKER":
				opLabel = "PANCK"
			if opLabel == "COLLAGENI":
				opLabel = "COLLAGEN"
			if opLabel == "CK7":
				opLabel = "PANCK"
			
			acquisitions.append({
				"Label":opLabel,
				"Metal":lbl.split("(")[0],
				"Ch": lbl.split("(")[1].replace(")",""),
				"Index":idx
			})
			idx+=1
	return acquisitions



def checkCommonCore(dLst):
	response = False
	channel_Names = [e['Label'] for e in dLst]
	#pprint(channel_Names)
	for mk in commonCoreList:
		if not mk in channel_Names:
			print("MISSING {}".format(mk))
			response = True
	return response


for f in inFiles:
	bsf = os.path.basename(f)
	outpath = Path(os.path.join(outdir,bsf))
	t = tf.TiffFile(f);
	nChannels = len(t.pages)
	print("Channels: {} in {}".format(nChannels,bsf))
	if nChannels <= 1:
		continue
	tiffHeader = ET.fromstring(t.pages[0].description)
	myMeta = formatMetadata(tiffHeader)

	mck = checkCommonCore(myMeta)
	if mck:
		break
		
	slst = []
	for m in myMeta:
		if m['Label'] in commonCoreList:
			imgMk = t.pages[m['Index']].asarray()
			slst.append(imgMk)

	newSubSet = np.array(slst)
	smsh = newSubSet.shape
	print("New Dim: {} x {} x {}".format(smsh[0],smsh[1],smsh[2]))
	imarr = xr.DataArray(newSubSet,
			dims=("c", "y", "x"),
			coords={"x": range(smsh[2]), "y": range(smsh[1]), 
					"c": commonCoreList}
		)
	Nc, Ny, Nx = imarr.shape
	smsh = imarr.shape
	print("XArray Dim: {} x {} x {}".format(smsh[0],smsh[1],smsh[2]))

	# Generate standard OME-XML
	channels_xml = '\n'.join(
		[f"""<Channel ID="Channel:0:{i}" Name="{commonCoreList[i]}" SamplesPerPixel="1"  ContrastMethod="Fluorescence" />"""
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
	tf.imwrite(outpath, data=imarr.values, description=xml, contiguous=True, resolution=(25400, 25400, "inch"))