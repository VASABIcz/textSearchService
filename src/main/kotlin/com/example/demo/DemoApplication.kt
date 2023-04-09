package com.example.demo

import com.fasterxml.jackson.databind.JsonNode
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.util.*


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

data class QueryResponse(val results: List<QueryResult>, val totalResults: Int)

data class RootQuery(val value: String, val data: JsonNode?, val forceInsert: Boolean = false)
data class RootChildQuery(val child: String, val value: String, val data: JsonNode?)

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}


@Component
class PostConstructExampleBean {
    @PostConstruct
    fun init() {

    }
}
