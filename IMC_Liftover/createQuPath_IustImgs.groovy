import groovy.io.FileType
import java.awt.image.BufferedImage
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.gui.commands.ProjectCommands
import qupath.imagej.processing.RoiLabeling
import qupath.lib.gui.QuPathGUI
import ij.IJ
import ij.gui.Wand
import ij.process.ImageProcessor

//omeDir="R:/PUBLIC/Hyperion_Imaging/Comfere_Nneke_MD/RaymondDevelopments/COMMONCORE"
//prjtDir="R:/PUBLIC/Hyperion_Imaging/Comfere_Nneke_MD/RaymondDevelopments/QuPath_ToSegment"
//omeDir="R:/PUBLIC/2022/02_February/hyperion_AF/COMMONCORE"
//prjtDir="R:/PUBLIC/2022/02_February/hyperion_AF/QuPathcc"
omeDir="R:/PUBLIC/2020/06_June/LaRusso_Project_02/COMMONCORE"
prjtDir="R:/PUBLIC/2020/06_June/LaRusso_Project_02/QuPathcc23"
// "C:\Users\M088378\AppData\Local\QuPath-0.2.3\QuPath-0.2.3 (console).exe" script "R:\PUBLIC\Hyperion_Imaging\Comfere_Nneke_MD\RaymondDevelopments\dev_code\createQuPath_IustImgs.groovy"
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

