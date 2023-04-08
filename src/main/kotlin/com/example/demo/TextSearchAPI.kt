package com.example.demo

interface TextSearchAPI {
    fun query(query: String, limit: Int? = 10): QueryResponse

    fun addChild(root: RootChildQuery)

    fun addRoot(root: RootQuery)
}