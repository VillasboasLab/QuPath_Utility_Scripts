//Prior to creating manual annotations described below, you may want to use your cell detection to generate unclassified cells.
//Run->RUN FOR PROJECT IF YOU HAVE MULTIPLE IMAGES - and only select those images that have validation objects

//STEP 1. Create Region* classified annotations across several images in your project that contain a good amount of cell class variation.
//STEP 2. Use the Points tool to place ground truth (what you believe the cell's class is) classes within each cell.
//STEP 3. Once all Points and Region* areas have been added to the project AND SAVED, run the script.

// Leave clearTestSetObjects as "true" so that the test set points you added are removed, and do not influence the classifier. They should be kept separate.

// You may want to modify these scripts to use a different folder if you create a third set of areas as a Validation set.

//Remove all the "stuff" like points 
clearTestSetObjects = true                      

trainingObjects = []
hierarchy = getCurrentHierarchy()
imageName = getProjectEntry().getImageName()
//Ensure there is an output directory to store object files
path = buildFilePath(PROJECT_BASE_DIR, "testSetPoints")
mkdirs(path)

//Save all of the points
path = buildFilePath(PROJECT_BASE_DIR, "testSetPoints", imageName+".testpts")
trainingPoints = hierarchy.getAnnotationObjects().findAll{it.getROI().isPoint()}
if(trainingPoints.size() > 0){
    new File(path).withObjectOutputStream {
        it.writeObject(trainingPoints)
    }
}

annotations = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Region*")}.collect {new qupath.lib.objects.PathAnnotationObject(it.getROI(), it.getPathClass())}

def gson = GsonTools.getInstance(true)

path = buildFilePath(PROJECT_BASE_DIR, "testSetPoints", imageName+".testarea")
File file = new File(path)
file.write(gson.toJson(annotations))


//CLEANUP
print "Done storing points into folder: " +path
if(clearTestSetObjects){
    removeObjects(trainingPoints, true)
    removeObjects(getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Region*")},true)
    fireHierarchyUpdate()
}

