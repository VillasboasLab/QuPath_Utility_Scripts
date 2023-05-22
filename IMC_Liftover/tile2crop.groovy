import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

//def outDir = "R:/PUBLIC/Hyperion_Imaging/Comfere_Nneke_MD/RaymondDevelopments/TileCrops"
//def outDir = "R:/PUBLIC/2022/04_April/Alexander_Req32321/TileCrops"
//def outDir = "R:/PUBLIC/2021/09_September/Hyperion_Bartemes/TileCrops"
def outDir = "O:/Hyperion_Imaging/Comfere_Nneke_MD/RaymondDevelopments/TileCrops"
def bfconvertExe = /Y:\Studies\Raymond\PyScripts_dev\bftools\bftools\bfconvert.bat/
def File logFile = new File(outDir+"/tile2crop_Sept2.log")
def tgt = 1
def outOf = 3


def x, y
def counter = 1
def plane = ImagePlane.getPlane(0, 0)
def roi, annotation
def server = getCurrentServer()
wd = server.getWidth()
ht = server.getHeight()
logFile << 'Dimensions: ' + wd + ' x ' + ht +"\n"
path = server.getPath()
imgName = URLDecoder.decode( path[path.lastIndexOf(':')+1..-1].tokenize("/")[-1].tokenize(".")[0], "UTF-8");
logFile << imgName+"\n"
String orignialImage = URLDecoder.decode( path[path.lastIndexOf(':')-1..-1].tokenize("[")[0] , "UTF-8");

File tmpFH = new File(orignialImage)
if (! tmpFH.exists()) {
    println "File Does not exist!"
    logFile << "File Does not exist!"
   // System.exit(42)
} else {
    println "File Found: "+orignialImage
}

//Define what you want to export here
def parentOfTiles = getAnnotationObjects().findAll{it.getPathClass() == null}
logFile << 'Parents: ' + parentOfTiles.size()

String cmd = ''
File directoryOUTPUT = new File(outDir)
if (directoryOUTPUT.exists())
{ 
    for (tile in parentOfTiles[0].getChildObjects() ) {
        if( counter % outOf == tgt){
            nom = tile.getName().replaceAll(' ','_')
            logFile << "IMAGE: "+nom
            imgName = imgName.replaceAll(' ','_').replaceAll('-','')        
            formattedFH = String.format( "%s_%s.ome.tiff", nom, imgName  )
            logFile << formattedFH+"\n"
        
            cropFile = outDir + '/' +formattedFH
            xTl = (int)tile.getROI().getBoundsX()
            yTl = (int)tile.getROI().getBoundsY()
            wTl = (int)tile.getROI().getBoundsWidth()
            hTl = (int)tile.getROI().getBoundsHeight()
            logFile << " x ="+xTl+" y ="+yTl+" W ="+wTl+" H ="+hTl+"\n"
            //orignialImage = orignialImage.replaceAll(' ','\\\\ ').replaceAll('-','\\\\-')
            cmd =  String.format( '%s -crop %d,%d,%d,%d "%s" %s',bfconvertExe,xTl,yTl,wTl,hTl,orignialImage,cropFile)
            //println cmd
            def proc = cmd.execute();
            def outputStream = new StringBuffer();            
            def errStream = new StringBuffer();
            proc.waitForProcessOutput(outputStream, errStream);
            logFile << outputStream.toString()+"\n"
            logFile << "EXE ERROR: "+errStream.toString()+"\n"
            println(counter+" Complete: "+formattedFH)
        }
        counter++
    }
}
println("Script Done")



