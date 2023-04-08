package com.example.demo

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoApplication

data class RootResult(
    val data: JsonNode?,
    val text: String,
    val counter: ULong
)

data class QueryResult(
    val matches: Collection<String>,
    val root: RootResult,
    val similarity: Double,
    val similarities: Collection<IntRange>
)

data class QueryResponse(val results: List<QueryResult>)

data class RootQuery(val value: String, val data: JsonNode)
data class RootChildQuery(val child: String, val value: String, val data: JsonNode)

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

