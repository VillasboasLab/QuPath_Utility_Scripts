import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

def nStart = 5
def nEnd = 40
//def outDir = "V:/PixelClassifierTrainingCrops"
//def outDir = "X:/SHARED/CODEX/Raymond/LungTMA_Crops"
def outDir = "X:/SHARED/CODEX/Raymond/LOGHAM_Spleen_Crops"
//def bfconvertExe = /Y:\Studies\Raymond\PyScripts_dev\bftools\bftools\bfconvert.bat/
def bfconvertExe = /Y:\Raymond\PyScripts_dev\bftools\bftools\bfconvert.bat/
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
        //println("Crop_${i+1}: " + x + "," + y)
        roi = ROIs.createRectangleROI(x, y, 200, 200, plane)
        annotation = PathObjects.createAnnotationObject(roi, getPathClass("Region*"))
        annotation.setName("Crop_${i+1}")
        annotation.setLocked(false)
        addObject(annotation)
    }
}

def proceed2 = Dialogs.showConfirmDialog("Crop Creation","Generate these OME.TIFFs?")
//print "You chose $proceed"
if( proceed2 ){
    File directoryOUTPUT = new File(outDir)
    String formattedFH = "example.ome.tiff"
    String path = server.getPath()
    String orignialImage = path[path.lastIndexOf(':')-1..-1].tokenize("[")[0]
    //println getCurrentImageData().getServer().getPath()
    String imgName = path[path.lastIndexOf(':')+1..-1].tokenize("/")[-1].tokenize(".")[0]
    //println imgName
    String cmd = ''
    if (directoryOUTPUT.exists())
    {
        getAnnotationObjects().each{
            print(it)
            it.setLocked(true)
            if( it.toString().contains('Rectangle') ){
                println(it.getName())
                if( it.getName().startsWith("Crop") ){
                    //println(it.getROI().getBoundsX())
                    formattedFH = String.format( "%s_%s.ome.tiff", it.getName(), imgName  )
                    println(formattedFH)
                    cropFile = outDir + '/' +formattedFH
                    cmd =  String.format( "%s -crop %d,%d,200,200 %s %s",bfconvertExe,(int)it.getROI().getBoundsX(),(int)it.getROI().getBoundsY(),orignialImage,cropFile)
                    println cmd
                    def proc = cmd.execute();
                    def outputStream = new StringBuffer();            
                    def errStream = new StringBuffer();
                    proc.waitForProcessOutput(outputStream, errStream);
                    println(outputStream.toString());            
                    println("EXE ERROR: "+errStream.toString());
                }
            }
        }
        //println "Run bat for BioFormats"
    } else {
        println("No Suitable Output Dir provided!")
    }
    
}

println("")
println("Done.")