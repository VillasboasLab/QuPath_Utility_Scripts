import qupath.lib.regions.ImagePlane
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.PointsROI;
import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathObjects

import qupath.lib.analysis.DistanceTools
import static qupath.lib.gui.scripting.QPEx.*

def plane = ImagePlane.getPlane(0, 0) 
def myProject = getProject()

def loadDir = /X:\SHARED\003 CODEX\Raymond\20220311_DSPTcellTMA3A_Feldman\OMERO_POINTS/
def ptSuffix = "-points.tsv"
//def loadDir = /Y:\AnalysisPipelineProjects\BMS_Tier2\POINTS/
//def loadDir = /Y:\AnalysisPipelineProjects\BMS_Aim1\QuPath_Projects_Raymond\POINTS/
//def ptSuffix = "-points.tsv"
def removePrevious = true

//myProject.getImageList().subList(0,2).each{
myProject.getImageList().each{
    String imgName = it.getImageName().split("\\.")[0]
    def imageData = it.readImageData()
    def hierarchy = imageData.getHierarchy()
    def server = imageData.getServer()
    
    //Find Corresponding PRED file
    def prefFH = new File(loadDir + "\\" + imgName + ptSuffix )
    if( prefFH.exists() ){
            println("Have File for "+imgName)
    } else {
        println("  >> SKIP: "+imgName)
        return
    }
    
    //Are there already points here? 
    trainingPoints = hierarchy.getAnnotationObjects().findAll{it.getROI().isPoint()}
    int p = trainingPoints.size()
    print 'Existing Points: '+p
    if(removePrevious) {
        println(" >> Wipe clean all Annotions!")
        //Wipeout
        hierarchy.removeObjects(trainingPoints, true)
        fireHierarchyUpdate()
    }
    
    annotObjects = []
    annoLines = prefFH.readLines()
    annoLines.remove(0)
    annoLines.each { String line ->
        lineFromFile = line.split("\t")
        if(lineFromFile.size() < 3){return}
        annoX = Float.parseFloat(lineFromFile[0]) 
        annoY = Float.parseFloat(lineFromFile[1]) 
        //println("x = "+annoX +"  y = "+annoY)
        ptName = lineFromFile[2]
        if(ptName?.trim()) {
            phenoClass = PathClass.fromString(ptName)            
        }

        def pt = new PointsROI(annoX,annoY)
        annotObjects << new PathAnnotationObject(pt, phenoClass)
    }
    hierarchy.addPathObjects(annotObjects)
    
    fireHierarchyUpdate()
    it.saveImageData(imageData)
    imageData.getServer().close() 
    println("")
    
}