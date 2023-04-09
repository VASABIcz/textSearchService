package com.example.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RequestMapping("/")
@RestController
class RestTextSearchAPI: TextSearchAPI {
    val dbRoot = RootNode()
    val sim = SimilarityCalculatorManager().apply {
        addCalculator(LevenshteinSimilarity(), 1.0)
        addCalculator(PopularitySimilarity { dbRoot.maxCounter }, 0.4)
    }
    val p: TextSearchAPI = RawTextSearchAPI(dbRoot, sim)

    init {
        val f = File("/home/vasabi/programing/kotlin/textSearch/src/main/kotlin/parsed.txt")
        val lines = f.readLines()
        lines.forEach {
            p.addRoot(RootQuery(it, null))
        }
        println("FINISHED!!")
    }

    @GetMapping("/query")
    override fun query(@RequestParam query: String, @RequestParam limit: Int?): QueryResponse {
        // println("q $query")
        val res =  p.query(query, limit)
        // println("Query done")
        return res
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