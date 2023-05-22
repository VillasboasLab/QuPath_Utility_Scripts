import ij.gui.Wand
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import static qupath.lib.gui.scripting.QPEx.*
import ij.IJ
import ij.process.ColorProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.tools.IJTools
import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.io.FileType
import java.awt.image.BufferedImage
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.gui.commands.ProjectCommands
import qupath.lib.gui.QuPathGUI
import ij.process.ImageProcessor
// Remove this if you don't need to generate new cell intensity measurements (it may be quite slow)
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathDetectionObject

regionSet="r"
prjtDir="X:/SHARED/003 CODEX/Cinnie_Projects/SecondaryDeliverables/20220203_LYMLEO452_PTLD_A_Villasboas/QuPath_Cluster_BN"
omeDir="X:/SHARED/003 CODEX/Raymond/20220203_LYMLEO452_PTLD_A_Villasboas_Redo/OMETIFF"
masksDir=/X:\SHARED\003 CODEX\Raymond\20220203_LYMLEO452_PTLD\SegmentationMasks\20220203_LYMLEO452_PTLD_A_Villasboas/
def outputPath = "NA"

//  qupath-0.2.3.jar
// java -jar -XX:MaxRAMPercentage=80.0 "C:\Users\M088378\AppData\Local\QuPath-0.3.2\app\qupath-app-0.3.2.jar" script "Y:\Raymond\GroovyScripts\createNewProject_OVTMA_v3_2.groovy"
 
// "C:\Users\M088378\AppData\Local\QuPath-0.2.2\QuPath-0.2.2 (console).exe" script "Y:\Studies\Raymond\GroovyScripts\createNewProject_OVTMA.groovy"
// "C:\Users\M088378\AppData\Local\QuPath-0.3.2\QuPath-0.3.2 (console).exe" script "Y:\Studies\Raymond\GroovyScripts\createNewProject_OVTMA_v3_2.groovy"
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

// Build a list of files
def files = []
selectedDir = new File(omeDir)
selectedDir.eachFileRecurse (FileType.FILES) { file ->
	if (file.getName().toLowerCase().endsWith(".ome.tiff"))
	{
		if(!file.getName().contains("Crop")){
			files << file
		}
	}
}

// Add files to the project
for (file in files) {
	def imagePath = file.getCanonicalPath()
	println(imagePath)
	
	// Get serverBuilder
	// def support = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, imagePath, "") Prior to v3.0
	def support = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, imagePath)
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
	def wholecellfiles = []
	directoryOfMasks.eachFileRecurse (FileType.FILES) { file ->
	if (file.getName().endsWith("_WholeCellMask.tiff"))
		{ wholecellfiles << file }
	}
	
	for (entry in project.getImageList()) {
		imgName = entry.getImageName()
		String sample = imgName[imgName.lastIndexOf(':')+1..-1].tokenize(".")[0]
		println(" >>> "+sample)
		def imageData = entry.readImageData()
		def server = imageData.getServer()
	
		//Mask File for Nuclei
		def nMask1 = wholecellfiles.find { it.getName().contains(sample) }
		if(nMask1 == null){
			println(" >>> MISSING MASK FILES!! <<<")
			println()
			continue
		}

		def imp = IJ.openImage(nMask1.absolutePath)
		int n = imp.getStatistics().max as int
		println("   Max Cell Label: "+n)
		if (n == 0) {
			print 'No objects found!'
			return
		}
		def ip = imp.getProcessor()
		if (ip instanceof ColorProcessor) {
			throw new IllegalArgumentException("RGB images are not supported!")
		}
		def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, n)
		def rois = roisIJ.collect {
			if (it == null)
				return
			return IJTools.convertToROI(it, 0, 0, downsample, plane);
		}
		rois = rois.findAll{null != it}
		// Convert QuPath ROIs to objects
		def pathObjects = rois.collect {
			return PathObjects.createDetectionObject(it)
		}
		println("   Number of PathObjects: "+pathObjects.size() )
		imageData.getHierarchy().addPathObjects(pathObjects)
		resolveHierarchy()
		entry.saveImageData(imageData)
		
		println(" >>> Calculating measurements...")
		println(imageData.getHierarchy())
		println("  DetectionObjects:"+imageData.getHierarchy().getDetectionObjects().size())		
		def measurements = ObjectMeasurements.Measurements.values() as List
		println(measurements)
		for (detection in imageData.getHierarchy().getDetectionObjects()) {
			ObjectMeasurements.addIntensityMeasurements( server, detection, downsample, measurements, [] )
			ObjectMeasurements.addShapeMeasurements( detection, server.getPixelCalibration(), ObjectMeasurements.ShapeFeatures.values() )
		}
		fireHierarchyUpdate()
		//imageData.resolveHierarchy()
		entry.saveImageData(imageData)
		imageData.getServer().close() // best to do this...
		println("")
	}
	System.gc()
}

project.syncChanges()

println("")
println("Done.")

