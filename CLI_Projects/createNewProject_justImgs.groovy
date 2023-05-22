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

//prjtDir="Y:/Rachel_INcell_images/QuPath_BMS_Dev/Mel38_BMS"
//omeDir="Y:/OME_TIFFs/BMS/Mel38_BMS"
//omeDir="X:/SHARED/CODEX/CODEX_OME_TIFFS/20211118_TNBCTMA2A_RLF"
//prjtDir="X:/SHARED/CODEX/Raymond/RobertoTNBC/TNBCTMA2A_RLF"
//omeDir="R:/PUBLIC/2021/09_September/Hyperion_Bartemes/COMMONCORE"
//prjtDir="R:/PUBLIC/2021/09_September/Hyperion_Bartemes/QuPathcc"
prjtDir="Y:/AnalysisPipelineProjects/BMS_Tier2/QuPath_Projects_Rachel/MEL45_BMS_2"
omeDir="Y:/AnalysisPipelineProjects/BMS_Tier2/OME_TIFF_Images/MEL45_BMS_2"
//omeDir="R:/PUBLIC/2020/06_June/LaRusso_Project_02/OMETIFFs"
//prjtDir="R:/PUBLIC/2020/06_June/LaRusso_Project_02/QuPath"

// "C:\Users\M088378\AppData\Local\QuPath-0.2.2\QuPath-0.2.2 (console).exe" script "Y:\Studies\Raymond\GroovyScripts\createNewProject_justImgs.groovy"
// "C:\Users\M088378\AppData\Local\QuPath-0.2.3\QuPath-0.2.3 (console).exe" script "Y:\Raymond\GroovyScripts\createNewProject_justImgs.groovy"
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
		files << file
	}
}

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
println("")
println("Done.")

