/**
 * Script to transfer QuPath objects from one image to another, applying an AffineTransform to any ROIs.
 * 
 * This is a based upon the script I posted on the epic forum thread https://forum.image.sc/t/interactive-image-alignment/23745/9?u=petebankhead
 * It has been updated for QuPath v0.2.0 (and made quite a lot shorter along the way).
 *
 * @author Pete Bankhead
 */

// SET ME! Define transformation matrix
// You can determine this using the 'Interactive image alignment' command
def matrix = [
        2.9595, -0.0526, -2446.0008,
        0.0526, 3.0205, -2992.6713
]

// SET ME! Define image containing the original objects (must be in the current project)
def otherImageName = 'S001_VHE_region_001.tif'

// SET ME! Delete existing objects
def deleteExisting = false

// SET ME! Change this if things end up in the wrong place
def createInverse = false

// SET ME! Change this to false if you want to discard existing measurements
// Note that things like area might be incorrect if the transform includes rescaling
boolean copyMeasurements = true


import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjectTools
import java.awt.geom.AffineTransform
import static qupath.lib.gui.scripting.QPEx.*

if (otherImageName == null) {
    Dialogs.showErrorNotification("Transform objects", "Please specify an image name in the script!")
    return
}

// Get the project & the requested image name
def project = getProject()
def entry = project.getImageList().find {it.getImageName() == otherImageName}
if (entry == null) {
    print 'Could not find image with name ' + otherImageName
    return
}

def otherHierarchy = entry.readHierarchy()
def pathObjects = otherHierarchy.getRootObject().getChildObjects()

// Define the transformation matrix
def transform = new AffineTransform(
        matrix[0], matrix[3], matrix[1],
        matrix[4], matrix[2], matrix[5]
)
if (createInverse)
    transform = transform.createInverse()

if (deleteExisting)
    clearAllObjects()

def newObjects = []
for (pathObject in pathObjects) {
    newObjects << transformObject(pathObject, transform, copyMeasurements)
}
addObjects(newObjects)

print 'Done!'

/**
 * Transform object, recursively transforming all child objects
 *
 * @param pathObject
 * @param transform
 * @return
 */
PathObject transformObject(PathObject pathObject, AffineTransform transform, boolean copyMeasurements) {
    def objectName = pathObject.getName()
    def newObject = PathObjectTools.transformObject(pathObject, transform, copyMeasurements)
    newObject.setName(objectName)
    // Handle child objects
    if (pathObject.hasChildren()) {
        newObject.addPathObjects(pathObject.getChildObjects().collect({transformObject(it, transform, copyMeasurements)}))
    }
    return newObject
}