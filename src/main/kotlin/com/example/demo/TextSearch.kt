package com.example.demo

import com.fasterxml.jackson.databind.JsonNode

fun Item.tokenize(): List<Item> {
    val tokens = mutableListOf<Item>()

    var start = 0

    val str = this.string

    str.forEachIndexed { index, c ->
        if (c.isWhitespace()) {
            if (index > start) {
                tokens.add(Item.Slice(IntRange(start, index-1), this))
            }
            start = index+1
        }
    }

    if (start < str.length) {
        tokens.add(Item.Slice(IntRange(start, str.length-1), this))
    }

    return tokens
}

fun Item.normalize(): Item {
    val str = this.string.lowercase()
    return Item.Modification(str, this)
}

fun Item.simplify(): Item {
    val res = this.string.mapNotNull { c ->
        if (c.isLetterOrDigit()) {
            c
        }
        else if (c.isWhitespace()) {
            " "
        } else {
            null
        }
    }
    val str = res.joinToString("")
    return Item.Modification(str, this)
}

fun Item.join(): Item {
    return Item.Modification(this.string.replace(" ", ""), this)
}

fun Item.lower(): Item {
    val str = this.string

    return Item.Modification(str.lowercase(), this)
}

fun Item.similar(value: String): Int {
    var sim = 0

    val str = this.string
    val min = minOf(str.length, value.length)

    for (i in 0 until min) {
        if (str[i] == value[i]) {
            sim++
        }
        else {
            break
        }
    }

    return sim
}

sealed class Item {
    class Root(val value: String, var data: JsonNode? = null): Item() {
        override val string: String
            get() = value

        override val length: Int
            get() = value.length
    }

    class Modification(val value: String, val source: Item): Item() {
        override val string: String
            get() = value

        override val length: Int
            get() = value.length
    }

    class Slice(val range: IntRange, val source: Item, val tinted: Boolean = false): Item() {
        override val string: String
            get() = source.string.slice(range)

        override val length: Int
            get() = range.last+1-range.first

        override fun slice(range: IntRange, tint: Boolean): Item {
            val newRange = this.range.first+range.first..this.range.first+range.last
            if (!this.tinted) {
                return Slice(range.first..range.last, this, tint)
            }

            if (newRange == this.range) {
                return source.slice(newRange, this.tinted)
            }
            return source.slice(newRange, tint)
        }
    }

    abstract val string: String
    abstract val length: Int
    var counter: ULong = 0u

    open fun slice(range: IntRange, tint: Boolean = false): Item {
        return Slice(range, this, tint)
    }

    val percentageOfOriginal: Double
        get() = when (this) {
            is Modification -> ((this.length.toDouble()/this.root.length.toDouble())*1.2).clamp(0.0, 1.0)
            is Root -> 1.0
            is Slice -> (this.length.toDouble()/this.root.length.toDouble()).clamp(0.0, 1.0)
        }

    val root: String
        get() = when (this) {
            is Root -> this.value
            is Modification -> this.source.root
            is Slice -> this.source.root
        }

    val daddy: Item
        get() = when (this) {
            is Modification -> this
            is Root -> this
            is Slice -> source.daddy
        }

    val untinted: Item
        get() = when (this) {
            is Modification -> this
            is Root -> this
            is Slice -> if (!this.tinted) this else this.source.untinted
        }

    val rootItem: Root
        get() = when (this) {
            is Root -> this
            is Modification -> this.source.rootItem
            is Slice -> this.source.rootItem
        }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this.hashCode() == other.hashCode()
    }

    override fun toString(): String {
        return this.string
    }
}

interface INode {
    fun print(spacing: Int = 0) {
        println(" ".repeat(spacing*2))
        children.forEach {
            it.print(spacing+1)
        }
    }

    val children: MutableList<Node>

    fun query(q: Item): Collection<Node> {
        val match = mutableListOf<Node>()

        for (c in children) {
            val sim = c.value.similar(q.string)

            if (sim == c.value.length) {
                if (sim == q.length) {
                    match.add(c)
                }

                val e = c.query(q.slice(sim until q.length))
                match.addAll(e)
            }
            // FIXME
            else if (sim == q.length) {
                match.add(c)
            }
        }

        return match
    }

    fun insert(value: Item) {
        assert(value.string.isNotEmpty())

        // println("inserting $value")
        //             index, amount
        var maxSim: Pair<Int, Int>? = null

        children.forEachIndexed { index, node ->
            val sim = node.value.similar(value.string)
            if (sim > (maxSim?.second ?: 0)) {
                maxSim = Pair(index, sim)
            }
        }
        assert(maxSim?.second != 0)

        // println("max sim ${maxSim?.second} $value ${children.map { it.value }}")

        if (maxSim == null) {
            // println("adding $value")
            this.children.add(Node(value))
        }
        else {
            // println("HAHA $value")
            val node = children[maxSim!!.first]

            node.slice(maxSim!!.second)

            // println("before")
            val d = value.slice(maxSim!!.second until  value.length, true)

            // println("d $d $value ${maxSim?.second}")
            node.insert(d)
        }
    }
}

data class RootNode(override val children: MutableList<Node> = mutableListOf()): INode {
    var maxCounter: ULong = 0u

    override fun query(q: Item): Collection<Node> {
        val res = super.query(q)
        res.forEach {
            it.value.counter++
            if (it.value.counter > maxCounter) {
                maxCounter = it.value.counter
            }
        }
        return res
    }
}

data class Node(var value: Item, override var children: MutableList<Node> = mutableListOf()): INode {
    override fun print(spacing: Int) {
        print(" ".repeat(spacing))
        println("<$value>")
        children.forEach {
            it.print(spacing+1)
        }
    }

    fun slice(amount: Int) {
        if (children.size == 0) {
            val old = value

            value = value.slice(0 until amount, true)

            val o = old.slice(amount until old.length, true)
            children.add(Node(o))
        }
        // FIXME this causes the database to be unnecessary deep but makes it work???
        else if (amount-1 != value.length) {
            val sl = value.slice(amount until value.length, true)
            if (sl.string == "") return
            val newNode = Node(sl, children)
            value = value.slice(0 until amount, true) // Item.Slice(0 until amount, value, true)
            children = mutableListOf(newNode)
        }
    }
}

fun prepareStr(str: String): Pair<Item.Root, Collection<Item>> {
    val result = mutableListOf<Item>()
    val root = Item.Root(str)
    result.add(root)

    val normalized = root.normalize()
    result.add(normalized)

    val simplified = root.simplify()
    result.add(simplified)

    val simplifiedNormalized = normalized.simplify()
    result.add(simplifiedNormalized)

    val simplifiedNormalizedJoined = simplifiedNormalized.join()
    result.add(simplifiedNormalizedJoined)

    val simplifiedJoined = simplified.join()
    result.add(simplifiedJoined)

    val normalizedJoined = normalized.join()
    result.add(normalizedJoined)

    result.addAll(root.tokenize())
    result.addAll(normalized.tokenize())
    result.addAll(simplified.tokenize())
    result.addAll(simplifiedNormalized.tokenize())

    return Pair(root, result)
}

fun countItems(items: List<Item>): List<Item> {
    val map = mutableMapOf<Item, Int>()

    for (item in items) {
        map.compute(item.rootItem) { it, value ->
            (value ?: 0) + item.length
        }
    }

    return map.map { it }.sortedByDescending { it.value }.map { it.key }
}