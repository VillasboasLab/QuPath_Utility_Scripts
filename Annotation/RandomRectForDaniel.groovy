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
    
//    clearAnnotations()
def proceed = Dialogs.showConfirmDialog("Crop Creation","Create new random crops?")
if( proceed ){
    def rnd = new Random()
    for (i = nStart; i < nEnd; i++) {
        x = 400+rnd.nextInt(wd-800)
        y = 200+rnd.nextInt(ht-400)
        println("Crop_${i+1}: " + x + "," + y)
        roi = ROIs.createRectangleROI(x, y, 200, 200, plane)
        annotation = PathObjects.createAnnotationObject(roi, getPathClass("Region*"))
        annotation.setName("Crop_${i+1}")
        annotation.setLocked(false)
        addObject(annotation)
    }
}