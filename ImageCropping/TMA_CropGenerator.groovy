import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

def outDir = "Y:/Studies/Rachel_INcell_images/QuPath_BMS_Dev/TrainingCrops"
def bfconvertExe = /Y:\Studies\Raymond\PyScripts_dev\bftools\bftools\bfconvert.bat/


def plane = ImagePlane.getPlane(0, 0)
def arr = [320, 620, 920, 1220, 1520]
File directoryOUTPUT = new File(outDir)


def project = getProject()
for (entry in project.getImageList()) {
    def imageData = entry.readImageData()
    def server = imageData.getServer()
    def counter = 0
    
    wd = server.getWidth()
    ht = server.getHeight()
    print 'Dimensions: ' + wd + ' x ' + ht    
    String path = server.getPath()
    String imgName = path[path.lastIndexOf(':')+1..-1].tokenize("/")[-1].tokenize(".")[0]
    println imgName
    String orignialImage = path[path.lastIndexOf(':')-1..-1].tokenize("[")[0]
    
    clearAnnotations()
    def newCrops = []
    for(idxY in arr){
        for(idxX in arr){
            //print "Y="+idxY+" X="+idxX
            roi = ROIs.createRectangleROI(idxX, idxY, 210, 210, plane)
            annotation = PathObjects.createAnnotationObject(roi, getPathClass("Region*"))
            annotation.setName("Crop_${counter+1}")
            annotation.setLocked(false)
            newCrops << annotation
            counter++
        }
    }
    imageData.getHierarchy().addPathObjects(newCrops)
    entry.saveImageData(imageData)

    String cmd = ''
    if (directoryOUTPUT.exists())
    {
        def annotCrops = imageData.getHierarchy().getAnnotationObjects()
        annotCrops.each{
            //println(it.getName())
            it.setLocked(true)
            //println(it.getROI().getBoundsX())
            formattedFH = String.format( "%s_%s.ome.tiff", it.getName(), imgName  )
            cropFile = outDir + '/' +formattedFH
            xTl = (int)it.getROI().getBoundsX()
            yTl = (int)it.getROI().getBoundsY()
            wTl = (int)it.getROI().getBoundsWidth()
            hTl = (int)it.getROI().getBoundsHeight()
            print "x ="+xTl+" y ="+yTl+" W ="+wTl+" H ="+hTl
            cmd =  String.format( "%s -crop %d,%d,%d,%d %s %s",bfconvertExe,xTl,yTl,wTl,hTl,orignialImage,cropFile)
            //println cmd
            def proc = cmd.execute();
            def outputStream = new StringBuffer();            
            def errStream = new StringBuffer();
            proc.waitForProcessOutput(outputStream, errStream);
            println(outputStream.toString());            
            println("EXE ERROR: "+errStream.toString());
        }
        //println "Run bat for BioFormats"
    } else {
        println("No Suitable Output Dir provided!")
    }
    imageData.getServer().close() // best to do this...

}



println("")
println("Done.")