import qupath.lib.regions.ImagePlane
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.EllipseROI;
import qupath.lib.objects.PathDetectionObject
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject

def plane = ImagePlane.getPlane(0, 0) 
proj = getProject()

def outputFileName = 'cellcounts.tsv'
new File(outputFileName).withWriter { writer ->
    proj.getImageList().each{
        String imgName = it.getImageName().split("\\.")[0]
        //println(imgName)
        imageData = it.readImageData()
        hierarchy = imageData.getHierarchy()
    
        int s = hierarchy.getDetectionObjects().size()
        String output = (imgName+'\t'+s)
        writer.writeLine output
    }
}