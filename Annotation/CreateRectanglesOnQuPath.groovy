import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

def nStart = 5
def nEnd = 7
def x, y
def counter = 0
def plane = ImagePlane.getPlane(0, 0)
def roi, annotation
def server = getCurrentServer()
wd = server.getWidth()
ht = server.getHeight()
print 'Dimensions: ' + wd + ' x ' + ht  


def array = new File("Y:/Studies/Daniel/region_coordinates.csv") as String[]  
//println(array[1])    
//    clearAnnotations()
def proceed = Dialogs.showConfirmDialog("Crop Creation","Create new crops?")
if( proceed ){
    
    for (i = 1; i < array.length; i++) {
        println(array[i])
        String[] separated = array[i].split(',')
        region = separated[0]
        x = Math.floor(Double.parseDouble(separated[1])).intValue()
        y = Math.floor(Double.parseDouble(separated[2])).intValue()
        //println(region)
        //println(x)
        //println(y)
        
        //x = 400+rnd.nextInt(wd-800)
        //y = 200+rnd.nextInt(ht-400)
        println(region + ": " + x + "," + y)
        roi = ROIs.createRectangleROI(x, y, 1000, 1000, plane)
        annotation = PathObjects.createAnnotationObject(roi, getPathClass("Region*"))
        annotation.setName(region)
        annotation.setLocked(false)
        addObject(annotation)
    }
}