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

def downsample = 1
double xOrigin = 0
double yOrigin = 0
ImagePlane plane = ImagePlane.getDefaultPlane()

//masksDir=/X:\SHARED\CODEX\Raymond\LOGHAM_Spleen_Masks\LastIteration/
def masksDir=/G:\Masks/
//def outputPath = "X:/SHARED/CODEX/Raymond/LOGHAM_Spleen_Masks/LastIteration/Spleen_Final_Quant.csv"
def outputPath = "G:/OV_region001_Quant.csv"

println("Discovering Mask Files...")
// Build 2 lists of mask files
def wholecellfiles = []
File directoryOfMasks = new File(masksDir)
directoryOfMasks.eachFileRecurse (FileType.FILES) { file ->
if (file.getName().endsWith("CellMask.tiff"))
    { wholecellfiles << file }
}

def project = getProject()
for (entry in project.getImageList()) {
    imgName = entry.getImageName()
    String sample = imgName[imgName.lastIndexOf(':')+1..-1].tokenize(".")[0]
    println(" >>> "+sample)
    def imageData = entry.readImageData()
    def server = imageData.getServer()

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

        println " >>> Laying down segmentation..."
        def pathObjects = []
        cellRois.eachWithIndex{ item, label ->
            if(cellRois[label] != null){
                def roiCellQ = IJTools.convertToROI(cellRois[label], xOrigin, yOrigin, downsample, plane)
                pathObjects << PathObjects.createDetectionObject(roiCellQ) 
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

    entry.saveImageData(imageData)
    imageData.getServer().close() // best to do this...
}

def imagesToExport = project.getImageList()
// Separate each measurement value in the output file with a tab ("\t")
def separator = ","
// Choose the type of objects that the export will process
// Other possibilities include:
//    1. PathAnnotationObject
//    2. PathDetectionObject
//    3. PathRootObject
// Note: import statements should then be modified accordingly
def exportType = PathCellObject.class
// Choose your *full* output path
def outputFile = new File(outputPath)
// Create the measurementExporter and start the export
def exporter  = new MeasurementExporter()
                  .imageList(imagesToExport)            // Images from which measurements will be exported
                  .separator(separator)                 // Character that separates values
                  .exportType(exportType)               // Type of objects to export
                  .exportMeasurements(outputFile)        // Start the export process


project.syncChanges()
println("Done.")

