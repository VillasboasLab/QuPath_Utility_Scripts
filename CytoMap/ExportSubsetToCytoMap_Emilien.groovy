import groovy.io.FileType
import java.awt.image.BufferedImage
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject

def outputPath = "Q:/Quantifications/TEMP/"
// "C:\Users\M088378\AppData\Local\QuPath-0.2.3\QuPath-0.2.3 (console).exe" script "Y:\Raymond\GroovyScripts\ExportSubsetToCytoMap_Emilien.groovy" --project="Q:\All_QuPathProjects\EH_1\project.qpproj"


// Multiple at once
//  use && between repeating commands, puts all output to single dir

def separator = ","
def exportType = PathCellObject.class
//def imagesToExport = project.getImageList()
def measurements = ObjectMeasurements.Measurements.values() as List
//println(getMeasurementList())
def compartments = ObjectMeasurements.Compartments.values() as List 
// println(compartments)

def columnsToInclude = new String[]{"Image","Name","Centroid X µm","Centroid Y µm",
	"CK19: Cell: Mean","CK19: Cell: Std.Dev.","CK19: Nucleus: Max", 
	"CD16: Cell: Max","LAMBDA: Cell: Median","KAPPA: Cell: Median",
	"DAPI: Nucleus: Std.Dev.","DAPI: Nucleus: Median",
	"NAKATPASE: Membrane: Median","NAKATPASE: Membrane: Std.Dev.",
	"ASMA: Cell: Mean","ASMA: Cell: Std.Dev.",
	"CD45: Cell: Mean","CD45: Cell: Median","CD45: Cell: Std.Dev.",
	"Nucleus: Area µm^2","Cell: Circularity","Cell: Solidity","Nucleus/Cell area ratio"}

def entry = getProjectEntry()
def server = getCurrentServer()
String path = server.getPath()
String sample = path[path.lastIndexOf(':')+1..-1].tokenize("/")[-1].tokenize(".")[0]
println( sample )

def outputFile = new File(outputPath+sample+".csv")
def exporter  = new MeasurementExporter()
			  .imageList([entry])			// Images from which measurements will be exported
			  .separator(separator)				 // Character that separates values
//			  .includeOnlyColumns(columnsToInclude) // Columns are case-sensitive
			  .exportType(exportType)			   // Type of objects to export
			  .exportMeasurements(outputFile)		// Start the export process


//println(exporter)
println "Done!"
