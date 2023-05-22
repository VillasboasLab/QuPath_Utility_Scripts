//Write out each region corresponding to an unclassified annotation
//Only use this if you have created SMALL unclassified annotations!!

def imageName = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName())

//Make sure the location you want to save the files to exists - requires a Project
def pathOutput = buildFilePath('Y://Studies//Daniel', imageName)
mkdirs(pathOutput)

unclassifiedAnnotations = getAnnotationObjects()
unclassifiedAnnotations.eachWithIndex{anno,x->
    

    
        print anno.getName()
        fileName = pathOutput+"//"+imageName+ "_"+anno.getName()+".png"
        
        //print fileName
        
        //Skip if the file already exists
        File testFile = new File(fileName)
        if(testFile.exists()){
            print "File already exists"
        }else{
            
            //For each annotation, we get its outline
            def roi = anno.getROI()
            //For each outline, we request the pixels within the bounding box of the annotation
            def requestROI = RegionRequest.createInstance(getCurrentServer().getPath(), 1, roi)
            //The 1 in the function above is the downsample, increase it for smaller images
            writeImageRegion(getCurrentServer(), requestROI, fileName)
        }
        
    
}

print "Done"
