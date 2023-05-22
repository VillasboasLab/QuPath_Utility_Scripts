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

masksDir=/R:\PUBLIC\2021\09_September\Hyperion_Bartemes\Trial_03\SEG_MASKS/

println("Discovering Mask Files...")
// Build 2 lists of mask files
def nucfiles = []
def wholecellfiles = []
File directoryOfMasks = new File(masksDir)
directoryOfMasks.eachFileRecurse (FileType.FILES) { file ->
if (file.getName().endsWith("NucleusMask.tiff"))
    { nucfiles << file }
}
directoryOfMasks.eachFileRecurse (FileType.FILES) { file ->
if (file.getName().endsWith("CellMask.tiff"))
    { wholecellfiles << file }
}

def project = getProject()
def server = getCurrentServer()
def imageData = getCurrentImageData()

String imgName = server.getPath()
String sample = imgName[imgName.lastIndexOf(':')+1..-1].tokenize("/")[-1].tokenize(".")[0]
println(" >>> "+sample)



//Mask File for Nuclei
def nMask1 = nucfiles.find { it.getName().contains(sample) }
println("Nuc Mask: "+nMask1)
def nucs = IJ.openImage(nMask1.getPath())
//println("Bit Depth = "+nucs.getBitDepth() )
println("[width, height, nChannels, nSlices, nFrames] = "+nucs.getDimensions() )
// Convert labels to ImageJ ROIs
def ipNuclei = nucs.getProcessor()
int n1 = ipNuclei.getStatistics().max as int
//println("Max Label Index = "+n1)
def nucleiRois  = RoiLabeling.labelsToConnectedROIs(ipNuclei, n1)
nucleiRois = nucleiRois - null    //.findAll{it != null}`
println "Number of Nuclei:"+nucleiRois.size()


//Mask File for Whole Cells
def wMask2 = wholecellfiles.find { it.getName().contains(sample) }
println("WC Mask: "+wMask2)
def imp = IJ.openImage(wMask2.getPath())
//println("Bit Depth = "+imp.getBitDepth() )
println("[width, height, nChannels, nSlices, nFrames] = "+imp.getDimensions() )
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



project.syncChanges()
println("Done.")

