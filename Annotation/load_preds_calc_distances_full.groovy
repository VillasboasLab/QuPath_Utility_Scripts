import qupath.lib.regions.ImagePlane
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.EllipseROI;
import qupath.lib.objects.PathDetectionObject
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject

import qupath.lib.analysis.DistanceTools
import static qupath.lib.gui.scripting.QPEx.*

def plane = ImagePlane.getPlane(0, 0) 
project = getProject()

//predDir = /R:\PUBLIC\2020\06_June\LaRusso_Project_02\QUPATH\external_predictions/
predDir = /R:\PUBLIC\2021\09_September\Hyperion_Bartemes\Trial_03\external_predictions/

//project.getImageList().subList(0,2).each{
project.getImageList().each{
    String imgName = it.getImageName().split("\\.")[0]
    def imageData = it.readImageData()
    def hierarchy = imageData.getHierarchy()
    def server = imageData.getServer()
    
    //Find Corresponding PRED file
    def prefFH = new File(predDir + "\\" + imgName +"_PRED.tsv" )
    if( prefFH.exists() ){
            println("Have Predictions for : "+imgName)
    } else {
        println("  >> SKIP: "+imgName)
        return
    }
    
    predLines = prefFH.readLines()
    predLines.remove(0)
    hierarchy.getCellObjects().eachWithIndex{ cell, idx ->
        phenoClass = predLines.get(idx).split("\t")[4]
        if(phenoClass != "Unknown"){
            cell.setPathClass(getPathClass(phenoClass))
        }
    }
    fireHierarchyUpdate()
    it.saveImageData(imageData)
    
    // Add distance calculations: essentially like the 'Distance to annotations 2D' command
    // Get all available classifications
    def pathClasses = hierarchy.getCellObjects().collect{it.getPathClass()} as Set
    if (!pathClasses) {
        println 'No classified annotations found!'
        return
    } else {
        int s = pathClasses.size()
        println('ClassSet: '+s)
    }
    // Parse pixel size info
    def cal = server.getPixelCalibration()
    String xUnit = cal.getPixelWidthUnit()
    String yUnit = cal.getPixelHeightUnit()
    double pixelWidth = cal.getPixelWidth().doubleValue()
    double pixelHeight = cal.getPixelHeight().doubleValue()
    if (!xUnit.equals(yUnit))
        throw new IllegalArgumentException("Pixel width & height units do not match! Width " + xUnit + ", height " + yUnit)
    String unit = xUnit
    println('Distance Unit: '+unit)
    
    
    ///Loop all cells
    //getDetectionObjects().eachWithIndex{ cell, idx ->
    def detections = hierarchy.getCellObjects()
    // Get annotations, grouped according to classification
    def allAnnotations = hierarchy.getCellObjects() //.findAll{it.isAnnotation()}
    // Measure all relevant distances for the specific core
    for (pathClass in pathClasses) {
        def classifiedAnnotations = allAnnotations.findAll {it.getPathClass() == pathClass}
        println("   "+pathClass+" : "+classifiedAnnotations.size())
        String name = "Distance to " + pathClass + " " + unit
        if (classifiedAnnotations)
            DistanceTools.centroidToCentroidDistance2D(detections, classifiedAnnotations, pixelWidth, pixelHeight, name)
        else
            detections.forEach {
                it.getMeasurementList().putMeasurement(name, Double.NaN)
                it.getMeasurementList().close()
            }
    }
    fireHierarchyUpdate()
    it.saveImageData(imageData)

    imageData.getServer().close() 
    println("")
}

// Changes should now be reflected in the project directory
project.syncChanges()





