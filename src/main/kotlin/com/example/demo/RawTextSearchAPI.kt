package com.example.demo

import info.debatty.java.stringsimilarity.NormalizedLevenshtein

interface SimilarityCalculator {
    fun calculateSimilarity(request: Item, result: Item): Double
}

fun <T: Comparable<T>> T.clamp(min: T, max: T): T {
    return if (this < min) {
        min
    }
    else if (this > max) {
        max
    }
    else {
        this
    }
}

class SimilarityCalculatorManager: SimilarityCalculator {
    private val calculators: MutableList<Pair<SimilarityCalculator, Double>> = mutableListOf()
    private var weightsSum: Double = 0.0

    override fun calculateSimilarity(request: Item, result: Item): Double {
        return calculators.map { it.first.calculateSimilarity(request, result).clamp(0.0, 1.0)*it.second }.sum()/weightsSum
    }

    fun addCalculator(calc: SimilarityCalculator, weight: Double) {
        val realWeight = weight.clamp(0.0, 1.0)
        calculators.add(Pair(calc, realWeight))
        weightsSum += realWeight
    }
}

class LevenshteinSimilarity: SimilarityCalculator {
    private var levenshtein = NormalizedLevenshtein()
    override fun calculateSimilarity(request: Item, result: Item): Double {
        return levenshtein.similarity(request.string, result.string)
    }
}

class PopularitySimilarity(val mostPopularGetter: () -> ULong): SimilarityCalculator {
    override fun calculateSimilarity(request: Item, result: Item): Double {
        val maxPopularity = mostPopularGetter()
        // FIXME
        return request.counter.toDouble()/maxPopularity.toDouble()
    }
}

class RawTextSearchAPI(private val node: RootNode, private val similarityCalculator: SimilarityCalculator): TextSearchAPI {
    override fun query(query: String, limit: Int?): QueryResponse {
        val roots = mutableMapOf<Item.Root, MutableSet<Item>>()
        val request = Item.Root(query)

        val rawResults = prepareStr(query).second.flatMap {
            node.query(it)
        }

        rawResults.forEach {
            val root = it.value.rootItem
            roots.compute(root) { k, v ->
                if (v == null) {
                    mutableSetOf(it.value)
                }
                else {
                    v.add(it.value)
                    null
                }
            }
        }

        val res = roots.map {
            Pair(it, similarityCalculator.calculateSimilarity(request, it.key))
        }.sortedByDescending {
            it.second
        }.take(limit ?: 10).map {
            val values = it.first.value
            val root = it.first.key
            val similarity = it.second

            val ranges = values.mapNotNull {
                if (it is Item.Slice) {
                    it.range
                }
                else {
                    null
                }
            }

            QueryResult(values.map { it.string }, RootResult(root.data, root.value, counter = root.counter), similarity, mergeRanges(ranges))
        }

        return QueryResponse(res)
    }

    override fun addChild(root: RootChildQuery) {
        val res = node.query(Item.Root(root.value, null))
        val first = res.firstOrNull()
        val dbRoot = if (first == null || first.value != res || first.value !is Item.Root) {
            val (dbRoot, modifications) = prepareStr(root.value)
            dbRoot.data = root.data

            modifications.forEach {
                node.insert(it)
            }

            dbRoot
        }
        else {
            first.value
        }
        node.insert(Item.Modification(root.child, dbRoot))
    }

    override fun addRoot(root: RootQuery) {
        val res = node.query(Item.Root(root.value, null))
        val first = res.firstOrNull()
        if (first == null || first.value != res || first.value !is Item.Root) {
            val (dbRoot, modifications) = prepareStr(root.value)
            dbRoot.data = root.data

            modifications.forEach {
                node.insert(it)
            }
        }
        else {
            first.value.rootItem.data = root.data
        }
    }
}

fun mergeRanges(ranges: Collection<IntRange>): Collection<IntRange> {
    if (ranges.isEmpty()) {
        return emptyList()
    }
    val maxRange = ranges.maxBy { it.last }.last
    val minRange = ranges.minBy { it.first }.first

    var currentRange: IntRange? = null
    val resRanges = mutableListOf<IntRange>()
    var end = 0
    var start = minRange

    for (i in minRange..maxRange) {
        if (currentRange == null || !currentRange.contains(i)) {
            val newRange = ranges.find { it.contains(i) }

            // newRange connects to current range
            if (currentRange != null && newRange != null && currentRange.last >= newRange.first-1) {
                end++
            }
            // new range doesnt connect to current range
            else if (newRange != null) {
                start = i
                end = i
            }
            // new range doesn't connect to current range and is null
            else {
                if (currentRange != null) {
                    resRanges.add(start..end)
                }
            }
            currentRange = newRange
        }
        else {
            end++
        }
    }
    if (currentRange != null) {
        resRanges.add(start..currentRange.last)
    }

    return resRanges
}

fun main() {
    val ranges = listOf(0..100, 20..80, 70..120, -10..0, 130..150, 100..140)
    val res = mergeRanges(ranges)
    println(res)
}