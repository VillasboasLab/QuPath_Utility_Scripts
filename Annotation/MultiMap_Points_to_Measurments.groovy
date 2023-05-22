import qupath.lib.regions.ImagePlane
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.EllipseROI;
import qupath.lib.objects.PathDetectionObject
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject

def plane = ImagePlane.getPlane(0, 0) 
def myproject = getProject()

def myCellIdexList = []
//myproject.getImageList().subList(0,10).each{
myproject.getImageList().each{
    String imgName = it.getImageName().split("\\.")[0]
    println(imgName)
    imageData = it.readImageData()
    hierarchy = imageData.getHierarchy()

    Set projectClassSet = hierarchy.getDetectionObjects().collect{it.getPathClass()} as Set
    //Set projectClassSet = hierarchy.getCellObjects().collect{it.getPathClass()} as Set
    int s = projectClassSet.size()
    println('ClassSet: '+s+'  '+projectClassSet)

    ///Create those fake ROI to do the "contains" look up
    trainingPoints = hierarchy.getAnnotationObjects().findAll{it.getROI().isPoint()}
    int p = trainingPoints.size()
    if( p == 0 ){return} //Skip non-annotated images, for now
    print 'trainingPoints: '+p
    println(trainingPoints)

    //Convert Points annotations into individual detection objects
    trainingObjects = []
    trainingPoints.each{ 
    //Cycle through all points within a points object
        pathClass = it.getPathClass()
        if(pathClass == null){
            if(it.getName() == null){print("Skip Blank Annotation Point")}
            else{
                pathClass = PathClassFactory.getPathClass(it.getName())
                println("Adding:"+it.getName()+" as Classified Points")
            }
        }
        it.getROI().getAllPoints().each{ 
        //for each point, create a circle on top of it that is "size" pixels in diameter
            x = it.getX()
            y = it.getY()
            size = 3
            //println(x+" x "+y)
            def roi = new EllipseROI(x-size/2,y-size/2,size,size, ImagePlane.getDefaultPlane())
            trainingObjects << PathObjects.createDetectionObject(roi, pathClass)
        }
    }
    hierarchy.addPathObjects(trainingObjects)
    trainingObjects.each{
        it.getMeasurementList().putMeasurement("GroundTruth",1)
    }


    // Loop all cells
    hierarchy.getDetectionObjects().eachWithIndex{ cell, idx ->
    //hierarchy.getCellObjects().eachWithIndex{ cell, idx ->
        roi = cell.getROI()
        if(cell.getMeasurementList().containsNamedMeasurement("GroundTruth")){return}
        whCell = hierarchy.getObjectsForROI(qupath.lib.objects.PathDetectionObject,roi)
        if(whCell.size() > 1) {
            def allLabels = []
            whCell.eachWithIndex{ rrrObj, ii ->
                if( rrrObj.getMeasurementList().containsNamedMeasurement("GroundTruth") ) {
                    allLabels << rrrObj.getPathClass()
                }
                //  ELSE should be cell.
            }
            lableList = allLabels.unique().sort()
            if(lableList.size() > 0) {
                def jNom = PathClass.fromString( lableList.join('|') )
                cell.setPathClass(jNom)
            } else {
                println("Empty:"+allLabels)
            }

        } else {
           cell.setPathClass(null)
        }
       
    }

    //Wipeout
    hierarchy.removeObjects(hierarchy.getDetectionObjects().findAll{it.getMeasurementList().containsNamedMeasurement("GroundTruth")}, true)
    fireHierarchyUpdate()
    
    it.saveImageData(imageData)
    imageData.getServer().close() 
    println("")
}
// Changes should now be reflected in the project directory
project.syncChanges()
    

println("")
println("Complete Class Writer.")

