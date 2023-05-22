import groovy.io.FileType
import java.awt.image.BufferedImage
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.gui.commands.ProjectCommands
import qupath.imagej.processing.RoiLabeling
import qupath.lib.gui.QuPathGUI
import ij.IJ
import ij.gui.Wand
import ij.process.ImageProcessor
// Remove this if you don't need to generate new cell intensity measurements (it may be quite slow)
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject

prjtDir="G:/Raymond_TEMP/BoneMarrowCodex"
def files = [ new File('X:/SHARED/CODEX/CODEX_OME_TIFFS/20200914_DLBCL_BM/reg003.ome.tiff') ]
masksDir=/X:\SHARED\CODEX\Raymond\LOGHAM_Spleen_Masks\LastIteration/
def outputPath = "X:/SHARED/CODEX/Raymond/LOGHAM_Spleen_Masks/LastIteration/Spleen_Final_Quant.csv"

//prjtDir="B:/SHARED/MC1776 melanoma/MxIF/QuPath_Projects/Tonsil12_SU2C"
//omeDir="B:/SHARED/MC1776 melanoma/MxIF/OME_TIFF_Images/Tonsil12_SU2C"
//masksDir=/B:\SHARED\MC1776 melanoma\MxIF\SegmentationMasks\Tonsil12_SU2C/
//prjtDir="C:/Users/M088378/Desktop/TestPrjt"
//prjtDir="V:/All_QuPathProjects/IH_37"
//prjtDir="Y:/Studies/Rachel_INcell_images/QuPath_BMS_Dev/MEL961_BMS"
//prjtDir="B:/SHARED/MC1776 melanoma/MxIF/QuPath_Projects/SU2C_1"
//prjtDir="X:/SHARED/CODEX/Raymond/DLBCL_BM_QuPath"
//masksDir="Y:/Studies/Ilastik_Masks/Melanoma/Mel14_4"
//masksDir='NONE'

// "C:\Users\M088378\AppData\Local\QuPath-0.2.2\QuPath-0.2.2 (console).exe" script "Y:\Studies\Raymond\GroovyScripts\createNewProject_singleROI.groovy"
// "C:\Users\M088378\AppData\Local\QuPath-0.2.3\QuPath-0.2.3 (console).exe" script "Y:\Raymond\GroovyScripts\createNewProject_singleROI.groovy"
def downsample = 1
double xOrigin = 0
double yOrigin = 0
ImagePlane plane = ImagePlane.getDefaultPlane()

File directory = new File(prjtDir)
if (!directory.exists())
{
	println("No project directory, creating one!")
	directory.mkdirs()
}

// Create project
def project = Projects.createProject(directory , BufferedImage.class)



// Add files to the project
for (file in files) {
	def imagePath = file.getCanonicalPath()
	println(imagePath)
	
	// Get serverBuilder
	def support = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, imagePath, "")
	def builder = support.builders.get(0)

	// Make sure we don't have null 
	if (builder == null) {
		print "Image not supported: " + imagePath
		continue
	}
	
	// Add the image as entry to the project
	print "Adding: " + imagePath
	entry = project.addImage(builder)
	
	// Set a particular image type
	def imageData = entry.readImageData()
	imageData.setImageType(ImageData.ImageType.FLUORESCENCE)
	entry.saveImageData(imageData)
	
	// Write a thumbnail if we can
	var img = ProjectCommands.getThumbnailRGB(imageData.getServer());
	entry.setThumbnail(img)
	
	// Add an entry name (the filename)
	entry.setImageName(file.getName())
}

// Changes should now be reflected in the project directory
project.syncChanges()


File directoryOfMasks = new File(masksDir)
if (directoryOfMasks.exists()){
	println("Discovering Mask Files...")
	// Build 2 lists of mask files
	def nucfiles = []
	def wholecellfiles = []

	directoryOfMasks.eachFileRecurse (FileType.FILES) { file ->
	if (file.getName().endsWith("NucleusMask.tiff"))
		{ nucfiles << file }
	}
	directoryOfMasks.eachFileRecurse (FileType.FILES) { file ->
	if (file.getName().endsWith("CellMask.tiff"))
		{ wholecellfiles << file }
	}
	
	for (entry in project.getImageList()) {
		imgName = entry.getImageName()
		String sample = imgName[imgName.lastIndexOf(':')+1..-1].tokenize(".")[0]
		println(" >>> "+sample)
		def imageData = entry.readImageData()
		def server = imageData.getServer()
	
		//Mask File for Nuclei
		def nMask1 = nucfiles.find { it.getName().contains(sample) }
		if(nMask1 == null){
			println(" >>> MISSING MASK FILES!! <<<")
			println()
			continue
		}
		def nucs = IJ.openImage(nMask1.getPath())
		//println("Bit Depth = "+nucs.getBitDepth() )
		//println("[width, height, nChannels, nSlices, nFrames] = "+nucs.getDimensions() )
		// Convert labels to ImageJ ROIs
		def ipNuclei = nucs.getProcessor()
		int n1 = ipNuclei.getStatistics().max as int
		//println("Max Label Index = "+n1)
		def nucleiRois  = RoiLabeling.labelsToConnectedROIs(ipNuclei, n1)
		nucleiRois = nucleiRois - null	//.findAll{it != null}`
		println "Number of Nuclei:"+nucleiRois.size()
	
	
		//Mask File for Whole Cells
		def wMask2 = wholecellfiles.find { it.getName().contains(sample) }
		def imp = IJ.openImage(wMask2.getPath())
		//println("Bit Depth = "+imp.getBitDepth() )
		//println("[width, height, nChannels, nSlices, nFrames] = "+imp.getDimensions() )
		def ipCells = imp.getProcessor()
		int n2 = ipCells.getStatistics().max as int
		//println("Max Label Index = "+n2)
		def cellRois  = RoiLabeling.labelsToConnectedROIs(ipCells, n2)
		println "Number of WholeCells:"+cellRois.size()
	
		/*
		* Add cells to Hierarchy
		*/
		if( cellRois.size() == nucleiRois.size() ){
			println " >>> Laying down segmentation..."
			def pathObjects = []
			nucleiRois.eachWithIndex{ item, label ->
				if(cellRois[label] != null){
					def roiNucQ = IJTools.convertToROI(nucleiRois[label], xOrigin, yOrigin, downsample, plane)
					def roiCellQ = IJTools.convertToROI(cellRois[label], xOrigin, yOrigin, downsample, plane)
					pathObjects << PathObjects.createCellObject(roiCellQ, roiNucQ, null, null) 
				} else {
					println("Null WholeCell Mask at "+label+". Skip!")
				}
			} // EOL: nucleiRois
			imageData.getHierarchy().addPathObjects(pathObjects)
			entry.saveImageData(imageData)
			
			print " >>> Calculating measurements..."
			print(imageData.getHierarchy())
			def measurements = ObjectMeasurements.Measurements.values() as List
			def compartments = ObjectMeasurements.Compartments.values() as List // Won't mean much if they aren't cells...
			for (detection in imageData.getHierarchy().getDetectionObjects()) {
				ObjectMeasurements.addIntensityMeasurements( server, detection, downsample, measurements, compartments )
				ObjectMeasurements.addShapeMeasurements( detection, server.getPixelCalibration(), ObjectMeasurements.ShapeFeatures.values() )
			}
			fireHierarchyUpdate()
	
		} else {
			println "Critical Error in Segemenation Mask count!"
		}
		entry.saveImageData(imageData)
		imageData.getServer().close() // best to do this...
	}

}

def imagesToExport = project.getImageList()
def separator = ","
def exportType = PathCellObject.class
def outputFile = new File(outputPath)
// Create the measurementExporter and start the export
def exporter  = new MeasurementExporter()
	.imageList(imagesToExport) // Images from which measurements will be exported
	.separator(separator) // Character that separates values
	.exportType(exportType) // Type of objects to export
	.exportMeasurements(outputFile) // Start the export process


project.syncChanges()
println("")
println("Done.")

