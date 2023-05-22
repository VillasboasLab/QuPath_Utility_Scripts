predictionFile=/Y:\AnalysisPipelineProjects\Tassos_NRP2\XGBPredictions\Mel48_AD_region_018_PRED.tsv/

predLines = new File(predictionFile).readLines()
predLines.remove(0)
getCellObjects().eachWithIndex{ cell, idx ->
    phenoClass = predLines.get(idx).split("\t")[3]
    println(phenoClass)
    cell.setPathClass(getPathClass(phenoClass))
}
fireHierarchyUpdate()