package io.ia.sdk.gradle.modl.util

import java.math.BigInteger
import java.util.ArrayDeque
import java.util.Deque
import java.util.Locale

interface Item {
    operator fun compareTo(item: Item?): Int
    val type: VersionItemType
    val isNull: Boolean
}

enum class VersionItemType {
    BigIntegerItem,
    StringItem,
    ListItem,
    IntItem,
    LongItem
}

class BigIntegerItem internal constructor(private val value: BigInteger) : Item {
    override val type = VersionItemType.BigIntegerItem

    constructor(stringValue: String) : this(stringValue.toBigInteger())

    override val isNull: Boolean
        get() = (BigInteger.ZERO == value)

    override fun compareTo(item: Item?): Int {
        item ?: return if (BigInteger.ZERO == value) 0 else 1 // 1.0 == 1, 1.1 > 1

        return when (item.type) {
            VersionItemType.IntItem,
            VersionItemType.LongItem -> 1
            VersionItemType.BigIntegerItem -> value.compareTo((item as BigIntegerItem).value)
            VersionItemType.StringItem -> 1 // 1.1 > 1-sp
            VersionItemType.ListItem -> 1 // 1.1 > 1-1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BigIntegerItem

        if (type != other.type) return false
        if (isNull != other.isNull) return false

        return value == other.value

    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + isNull.hashCode()
        return result
    }

    override fun toString(): String {
        return value.toString()
    }
}

/**
 * Represents a numeric item in the version item list that can be represented with a long.
 */
class LongItem constructor(private val value: Long = 0L) : Item {
    override val type: VersionItemType = VersionItemType.LongItem

    constructor(str: String) : this(str.toLong())

    override val isNull: Boolean
        get() = value == 0L

    override fun compareTo(item: Item?): Int {
        item ?: return if (value == 0L) 0 else 1 // 1.0 == 1, 1.1 > 1

        return when (item.type) {
            VersionItemType.IntItem -> 1
            VersionItemType.LongItem -> {
                val itemValue = (item as LongItem).value
                value.compareTo(itemValue)
            }
            VersionItemType.BigIntegerItem -> -1
            VersionItemType.StringItem -> 1 // 1.1 > 1-sp
            VersionItemType.ListItem -> 1 // 1.1 > 1-1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val longItem = other as LongItem
        return value == longItem.value
    }

    override fun hashCode(): Int {
        return (value xor (value ushr 32)).toInt()
    }

    override fun toString(): String {
        return java.lang.Long.toString(value)
    }
}

/**
 * Represents a string in the version item list, usually a qualifier.
 */
class StringItem constructor(private val rawValue: String, followedByDigit: Boolean) : Item {
    override val type: VersionItemType = VersionItemType.StringItem
    private var effectiveValue: String

    companion object {
        private val ALIASES = mapOf(
            "ga" to "",
            "final" to "",
            "release" to "",
            "cr" to "rc"
        )
        private val QUALIFIERS = setOf("alpha", "beta", "milestone", "rc", "snapshot", "", "sp")

        /**
         * A comparable value for the empty-string qualifier. This one is used to determine if a given qualifier makes
         * the version older than one without a qualifier, or more recent.
         */
        private val RELEASE_VERSION_INDEX = QUALIFIERS.indexOf("").toString()

        /**
         * Returns a comparable value for a qualifier.
         *
         * This method takes into account the ordering of known qualifiers then unknown qualifiers with lexical
         * ordering.
         *
         * just returning an Integer with the index here is faster, but requires a lot of if/then/else to check for -1
         * or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by the first character,
         * so this is still fast. If more characters are needed then it requires a lexical sort anyway.
         *
         * @param qualifier
         * @return an equivalent value that can be used with lexical comparison
         */
        fun comparableQualifier(qualifier: String): String {
            val i = QUALIFIERS.indexOf(qualifier)
            return if (i == -1) QUALIFIERS.size.toString() + "-" + qualifier else i.toString()
        }
    }

    init {
        if (followedByDigit && rawValue.length == 1) {
            // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
            effectiveValue = when (rawValue[0]) {
                'a' -> "alpha"
                'b' -> "beta"
                'm' -> "milestone"
                'r' -> "rc"
                else -> ""
            }
        }
        this.effectiveValue = ALIASES[rawValue] ?: rawValue
    }

    override val isNull: Boolean
        get() = (comparableQualifier(rawValue).compareTo(RELEASE_VERSION_INDEX) == 0)

    override fun compareTo(item: Item?): Int {
        // 1-rc < 1, 1-ga > 1
        item ?: return comparableQualifier(rawValue).compareTo(RELEASE_VERSION_INDEX)

        return when (item.type) {
            VersionItemType.IntItem,
            VersionItemType.LongItem,
            VersionItemType.BigIntegerItem -> -1 // 1.any < 1.1 ?
            VersionItemType.StringItem -> comparableQualifier(rawValue).compareTo(comparableQualifier((item as StringItem).rawValue))
            VersionItemType.ListItem -> -1 // 1.any < 1-1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as StringItem
        return rawValue == that.rawValue
    }

    override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    override fun toString(): String {
        return rawValue
    }
}

/**
 * Represents a numeric item in the version item list that can be represented with an int.
 */
class IntItem(private val value: Int = 0) : Item {
    override val type: VersionItemType = VersionItemType.IntItem

    constructor(str: String) : this(str.toInt())

    override val isNull: Boolean
        get() = value == 0

    override fun compareTo(item: Item?): Int {
        item ?: return if (value == 0) 0 else 1 // 1.0 == 1, 1.1 > 1

        when (item.type) {
            VersionItemType.IntItem -> {
                val itemValue = (item as IntItem).value
                return value.compareTo(itemValue)
            }
            VersionItemType.LongItem,
            VersionItemType.BigIntegerItem -> return -1
            VersionItemType.StringItem -> return 1 // 1.1 > 1-sp
            VersionItemType.ListItem -> return 1   // 1.1 > 1-1
            else -> throw IllegalStateException("invalid item: " + item.javaClass)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val intItem = other as IntItem
        return value == intItem.value
    }

    override fun hashCode(): Int {
        return value
    }

    override fun toString(): String {
        return Integer.toString(value)
    }

    companion object {
        val ZERO = IntItem()
    }
}

/**
 * Represents a version list item. This class is used both for the global item list and for sub-lists (which start
 * with '-(number)' in the version specification).
 */
private class ListItem() : ArrayList<Item>(), Item {
    override val type: VersionItemType = VersionItemType.ListItem
    override val isNull: Boolean
        get() = (size == 0)

    fun normalize() {
        for (i in size - 1 downTo 0) {
            val lastItem = get(i)
            if (lastItem.isNull) {
                // remove null trailing items: 0, "", empty list
                removeAt(i)
            } else if (lastItem !is ListItem) {
                break
            }
        }
    }

    override fun compareTo(item: Item?): Int {
        if (item == null) {
            if (size == 0) {
                return 0 // 1-0 = 1- (normalize) = 1
            }
            // Compare the entire list of items with null - not just the first one
            for (i: Item? in this) {
                val result = i?.compareTo(null) ?: continue
                if (result != 0) return result

            }
            return 0
        }

        when (item.type) {
            VersionItemType.IntItem,
            VersionItemType.LongItem,
            VersionItemType.BigIntegerItem -> return -1 // 1-1 < 1.0.x
            VersionItemType.StringItem -> return 1 // 1-1 > 1-sp
            VersionItemType.ListItem -> {
                val left: Iterator<Item?> = iterator()
                val right: Iterator<Item?> = (item as ListItem).iterator()
                while (left.hasNext() || right.hasNext()) {
                    val l = if (left.hasNext()) left.next() else null
                    val r = if (right.hasNext()) right.next() else null

                    // if this is shorter, then invert the compare and mul with -1
                    val result = l?.compareTo(r) ?: if (r == null) 0 else {
                        r.compareTo(l) * -1
                    }

                    if (result != 0) {
                        return result
                    }
                }
                return 0
            }
            else -> throw IllegalStateException("invalid item: " + item.javaClass)
        }
    }

    override fun toString(): String {
        val buffer = StringBuilder()
        for (item: Item in this) {
            if (buffer.isNotEmpty()) {
                buffer.append(if (item is ListItem) '-' else '.')
            }
            buffer.append(item)

        }
        return buffer.toString()
    }

    /**
     * Return the contents in the same format that is used when you call toString() on a List.
     */
    fun toListString(): String {
        val buffer = StringBuilder()
        buffer.append("[")
        for (item: Item in this) {
            if (buffer.length > 1) {
                buffer.append(", ")
            }
            if (item is ListItem) {
                buffer.append(item.toListString())
            } else {
                buffer.append(item)
            }
        }
        buffer.append("]")
        return buffer.toString()
    }


}

/**
 *
 * This class and the corresponding tests were ported from the java implementation which exists in the open
 * source, Apache 2 licensed, Maven project. For full attribution of the original Java implementation, see
 * [the Maven repo on github](https://github.com/apache/maven).
 *
 * The original author in that repository is noted as [Hervé Boutemy](mailto:hboutemy@apache.org)
 */
public class ComparableVersion(private var value: String) : Comparable<ComparableVersion?> {
    private var items: ListItem = ListItem()
    private var _canonical:String? = null

    fun getCanonical(): String {
        if (_canonical == null) {
            _canonical = items.toString()
        }
        return _canonical as String
    }

    init {
        parseVersion(value)
    }

    fun parseVersion(version: String) {
        value = version
        val ver = version.lowercase(Locale.ENGLISH)
        items = ListItem()
        var list = ListItem()
        val stack: Deque<Item> = ArrayDeque<Item>().apply {
            this.push(items)
        }

        var isDigit = false
        var startIndex = 0

        for (i in ver.indices) {
            val c = ver[i]
            println("parseVersion:381: c is $c")
            if (c == '.') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO)
                } else {
                    list.add(parseItem(isDigit, ver.substring(startIndex, i)))
                }
                startIndex = i + 1
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO)
                } else {
                    list.add(parseItem(isDigit, ver.substring(startIndex, i)))
                }
                startIndex = i + 1
                list.add(ListItem().also { list = it })
                stack.push(list)
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    list.add(StringItem(ver.substring(startIndex, i), true))
                    startIndex = i
                    list.add(ListItem().also { list = it })
                    stack.push(list)
                }
                isDigit = true
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, ver.substring(startIndex, i)))
                    startIndex = i
                    list.add(ListItem().also { list = it })
                    stack.push(list)
                }
                isDigit = false
            }
            println("parseVersion: forLoop index $i\nlist.toString(): ${list.toString()}\nlist.toListString(): ${list.toListString()}\n stack: ${stack.map { it.toString()}}")
        }
        if (ver.length > startIndex) {
            println("Adding ${ver.substring(startIndex)}")
            list.add(parseItem(isDigit, ver.substring(startIndex)))
        }
        while (!stack.isEmpty()) {
            list = stack.pop() as ListItem
            println("pre-normalized: $list")
            list.normalize()
            println("    normalized: $list")
        }
    }

    override operator fun compareTo(other: ComparableVersion?): Int {
        if (other == null) {
            throw Exception("Cannot compare ComparableVersion to null value")
        }

        return items.compareTo(other.items)
    }

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return other is ComparableVersion && items == other.items
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }

    companion object {
        private const val MAX_INTITEM_LENGTH = 9
        private const val MAX_LONGITEM_LENGTH = 18
        private fun parseItem(isDigit: Boolean, buf: String): Item {
            var buffer = buf
            if (isDigit) {
                buffer = stripLeadingZeroes(buffer)
                if (buffer.length <= MAX_INTITEM_LENGTH) {
                    // lower than 2^31
                    return IntItem(buffer)
                } else if (buffer.length <= MAX_LONGITEM_LENGTH) {
                    // lower than 2^63
                    return LongItem(buffer)
                }
                return BigIntegerItem(buffer)
            }
            return StringItem(buffer, false)
        }

        private fun stripLeadingZeroes(buf: String?): String {
            if (buf == null || buf.isEmpty()) {
                return "0"
            }
            for (i in 0 until buf.length) {
                val c = buf[i]
                if (c != '0') {
                    return buf.substring(i)
                }
            }
            return buf
        }
        // CHECKSTYLE_OFF: LineLength
        /**
         * Main to test version parsing and comparison.
         *
         *
         * To check how "1.2.7" compares to "1.2-SNAPSHOT", for example, you can issue
         * <pre>java -jar ${maven.repo.local}/org/apache/maven/maven-artifact/${maven.version}/maven-artifact-${maven.version}.jar "1.2.7" "1.2-SNAPSHOT"</pre>
         * command to command line. Result of given command will be something like this:
         * <pre>
         * Display parameters as parsed by Maven (in canonical form) and comparison result:
         * 1. 1.2.7 == 1.2.7
         * 1.2.7 &gt; 1.2-SNAPSHOT
         * 2. 1.2-SNAPSHOT == 1.2-snapshot
        </pre> *
         *
         * @param args the version strings to parse and compare. You can pass arbitrary number of version strings and always
         * two adjacent will be compared
         */
        // CHECKSTYLE_ON: LineLength
        @JvmStatic
        fun main(args: Array<String>) {
            println(
                "Display parameters as parsed by Maven (in canonical form and as a list of tokens) and"
                    + " comparison result:"
            )
            if (args.isEmpty()) {
                return
            }
            var prev: ComparableVersion? = null
            var i = 1
            for (version: String in args) {
                val c = ComparableVersion(version)
                if (prev != null) {
                    val compare = prev.compareTo(c)
                    println(
                        ("   " + prev.toString() + ' '
                            + (if ((compare == 0)) "==" else (if ((compare < 0)) "<" else ">")) + ' ' + version)
                    )
                }
                println(
                    ((i++).toString() + ". " + version + " -> " + c.getCanonical()
                        + "; tokens: " + c.items.toListString())
                )
                prev = c
            }
        }
    }
}
