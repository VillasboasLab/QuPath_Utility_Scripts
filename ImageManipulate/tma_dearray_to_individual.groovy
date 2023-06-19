import qupath.lib.regions.RegionRequest
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.images.servers.*
import javax.imageio.*
import qupath.lib.images.servers.ImageServerMetadata;

// Define resolution - 1.0 means full size
double downsample = 1.0 //Downsample factor when exporting raw images and their overlays
output_fmt='.tif' //Supported formats include .ome.tif, .png, .tif, .jpg. Default compression scheme for each format

// Create output directory inside the project
def dirOutput = buildFilePath(PROJECT_BASE_DIR, 'cores_raw')
mkdirs(dirOutput)

// Write the cores
def server = getCurrentImageData().getServer()
def path = server.getPath()
for (core in getTMACoreList()) {
    outFh = new File(dirOutput, core.getName() + output_fmt)
    if(outFh.exists()) {
       continue 
    }
    println(core.getName()) 
    // Stop if Run -> Kill running script is pressed   
    if (Thread.currentThread().isInterrupted())
        break
    // Write the image
    annoRoi = core.getROI()
    def requestROI = RegionRequest.createInstance(getCurrentServer().getPath(), 1, annoRoi)
    writeImageRegion(getCurrentServer(), requestROI, outFh.absolutePath)
}
print('Finished exporting individual core screenshots for current opened image')