import groovy.io.FileType
import java.awt.image.BufferedImage
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.gui.commands.ProjectCommands
import qupath.imagej.processing.RoiLabeling
import qupath.lib.gui.QuPathGUI
import ij.IJ
import ij.process.ImageProcessor
import qupath.lib.objects.PathCellObject;
import qupath.lib.objects.PathDetectionObject;


cytomapDir=/G:\cd8-ovtma-dev\Outpu/

def cmfiles = []
File directoryOfPred = new File(cytomapDir)
directoryOfPred.eachFileRecurse (FileType.FILES) { file ->
if (file.getName().startsWith("CytoMAP_"))
    { cmfiles << file }
}


def project = getProject()
for (entry in project.getImageList()) {
    imgName = entry.getImageName()
    String sample = imgName[imgName.lastIndexOf(':')+1..-1].tokenize(".")[0]
    println(" >>> "+sample)
    def imageData = entry.readImageData()
    def server = imageData.getServer()

    //Pred File for this ROI
    def nPred1 = cmfiles.find { it.getName().contains(sample) }
    if(nPred1 == null){continue}
    println(nPred1)
    
    predLines = new File(nPred1.getPath()).readLines()
    predLines.remove(0)
    //println(imageData.getHierarchy())
    //println(imageData.getHierarchy().getDetectionObjects())
    imageData.getHierarchy().getDetectionObjects().eachWithIndex{ cell, idx ->
        phenoIndex = predLines.get(idx).split(",")[7]
        //println(phenoIndex)
        if(phenoIndex == "3"){
           cell.setPathClass(getPathClass("CD8+"))
        }
        
    }
    fireHierarchyUpdate()
    entry.saveImageData(imageData)
    imageData.getServer().close() // best to do this...
}

