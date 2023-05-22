predictionFile='Y:/Studies/AnalysisPipelineProjects/MelPanel4/Reports/HLA1_Classify2/CytoMAP_Sample_Mel4_4_region_008.csv'
predLines = new File(predictionFile).readLines()
predLines.remove(0)
getCellObjects().eachWithIndex{ cell, idx ->
    phenoIndex = predLines.get(idx).split(",")[4]
    println(phenoIndex)
    phenoClass = "Negative"
    if(phenoIndex == "2"){
        phenoClass = "Positive"
    }
    cell.setPathClass(getPathClass(phenoClass))
}
fireHierarchyUpdate()