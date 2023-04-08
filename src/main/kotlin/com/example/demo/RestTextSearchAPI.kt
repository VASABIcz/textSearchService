package com.example.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/")
@RestController
class RestTextSearchAPI: TextSearchAPI {
    val dbRoot = RootNode()
    val sim = SimilarityCalculatorManager().apply {
        addCalculator(LevenshteinSimilarity(), 1.0)
        addCalculator(PopularitySimilarity { dbRoot.maxCounter }, 0.4)
    }
    val p: TextSearchAPI = RawTextSearchAPI(dbRoot, sim)

    @GetMapping("/query")
    override fun query(@RequestParam query: String, @RequestParam limit: Int?): QueryResponse {
        return p.query(query, limit)
    }

    @PostMapping("/addChild")
    override fun addChild(@RequestBody root: RootChildQuery) {
        return p.addChild(root)
    }

    @PostMapping("/addRoot")
    override fun addRoot(@RequestBody root: RootQuery) {
        return p.addRoot(root)
    }
}