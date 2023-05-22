import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

def outDir = "X:/SHARED/CODEX/Raymond/LOGHAM_DLBCL_BM_Crops"
def bfconvertExe = /Y:\Raymond\PyScripts_dev\bftools\bftools\bfconvert.bat/

def x, y
def counter = 0
def plane = ImagePlane.getPlane(0, 0)
def roi, annotation
def server = getCurrentServer()
wd = server.getWidth()
ht = server.getHeight()
print 'Dimensions: ' + wd + ' x ' + ht    
String path = server.getPath()
String imgName = path[path.lastIndexOf(':')+1..-1].tokenize("/")[-1].tokenize(".")[0]
println imgName
String orignialImage = path[path.lastIndexOf(':')-1..-1].tokenize("[")[0]

//Define what you want to export here
def parentOfTiles = getAnnotationObjects().findAll{it.getPathClass() == null}
print 'Parents: ' + parentOfTiles.size()

String cmd = ''
File directoryOUTPUT = new File(outDir)
if (directoryOUTPUT.exists())
{
    for (tile in parentOfTiles[0].getChildObjects() ) {
        nom = tile.getName().replaceAll(' ','_')
        //print nom
        formattedFH = String.format( "%s_%s.ome.tiff", nom, imgName  )
        println(formattedFH)
        
        cropFile = outDir + '/' +formattedFH
        xTl = (int)tile.getROI().getBoundsX()
        yTl = (int)tile.getROI().getBoundsY()
        wTl = (int)tile.getROI().getBoundsWidth()
        hTl = (int)tile.getROI().getBoundsHeight()
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
}