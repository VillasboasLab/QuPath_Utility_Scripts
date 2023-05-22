predictionFile=/Y:\AnalysisPipelineProjects\FunctionalPredictions_StandAlone\Mel30_BMS_region_020_LAG3_only.csv/

predLines = new File(predictionFile).readLines()
predLines.remove(0)
getCellObjects().eachWithIndex{ cell, idx ->
    phenoClass = predLines.get(idx).split(",")[3]
    if(phenoClass != "Unknown"){
        print(phenoClass)
        cell.setPathClass(getPathClass(phenoClass))
    }
    
}
fireHierarchyUpdate()
