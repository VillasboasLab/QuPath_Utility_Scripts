predictionFile=/R:\PUBLIC\2021\09_September\Hyperion_Bartemes\Trial_04\external_predictions\31365_0UHK-LN_ROI_001_PRED.tsv/

predLines = new File(predictionFile).readLines()
predLines.remove(0)
getCellObjects().eachWithIndex{ cell, idx ->
    phenoClass = predLines.get(idx).split("\t")[4]
    println(phenoClass)
    cell.setPathClass(getPathClass(phenoClass))
}
fireHierarchyUpdate()