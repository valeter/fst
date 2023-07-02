package engine

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author valter
 */
class InMemoryPieceTable(
    private val original: String,
    private val added: StringBuilder,
    undoDepth: Int = EngineSettings.defaultUndoDepth
) {
    private val version = AtomicInteger(0)
    private val lastVersion = AtomicLong(0L)

    private var nodes: LinkedList<Node> =
        if (original.isEmpty()) LinkedList() else LinkedList(listOf(rootNode(original)))

    private val versions: Array<VersionedState?> =
        Array(undoDepth + 1) { i -> if (i == 0) VersionedState(0L, LinkedList(nodes)) else null }


    // 0 - 1 - 2 - 3 - 4 - 5 - 6
    //                  \
    //                   7 - 8 - 9
    // [0, 1, 2, 3, 4, 5, 6*]
    // [0, 1, 2, 3, 4*, 5, 6]
    // [0, 1, 2, 3, 4, 7*, 6]
    // [9*, 1, 2, 3, 4, 7, 8]
    fun undo() {
        if (versions[version.get()] == null) {
            throw IllegalArgumentException("Unreachable statement")
        }
        val prevVersion = (version.get() + versions.size - 1) % versions.size
        if (versions[prevVersion] == null) {
            return
        }
        if (versions[prevVersion]!!.version > versions[version.get()]!!.version) {
            // max undoDepth reached
            return
        }
        version.set(prevVersion)
        nodes = LinkedList(versions[prevVersion]!!.state)
    }

    fun redo() {
        if (versions[version.get()] == null) {
            throw IllegalArgumentException("Unreachable statement")
        }
        val nextVersion = (version.get() + 1) % versions.size
        if (versions[nextVersion] == null) {
            return
        }
        if (versions[nextVersion]!!.version < versions[version.get()]!!.version) {
            // top of undoDepth stack reached
            return
        }
        version.set(nextVersion)
        nodes = LinkedList(versions[nextVersion]!!.state)
    }

    fun insert(s: String, insertInd: Int) {
        if (s.isEmpty()) {
            return
        }
        validateInd(insertInd)
        val newNode = Node(NodeType.ADDED, added.length, s.length)
        added.append(s)
        var curPieceLastInd = 0
        val iter = nodes.listIterator()
        var curPiece: Node? = null
        while (insertInd > curPieceLastInd && iter.hasNext()) {
            curPiece = iter.next()
            curPieceLastInd += curPiece.length
        }
        if (curPieceLastInd == insertInd) {
            iter.add(newNode)
        } else if (curPiece != null) {
            iter.remove()
            val split1Length = curPiece.length - (curPieceLastInd - insertInd)
            val splitNode1 = Node(curPiece.type, curPiece.start, split1Length)
            val splitNode2 = Node(curPiece.type, curPiece.start + split1Length, curPieceLastInd - insertInd)
            iter.add(splitNode1)
            iter.add(newNode)
            iter.add(splitNode2)
        } else {
            throw IllegalStateException("Unreachable statement")
        }
        fixVersion()
    }

    fun delete(deleteIndFrom: Int, length: Int) {
        if (length == 0) {
            return
        }
        if (length < 0) {
            throw IllegalArgumentException("Length of deleted text should not be < 0 (actual $length)")
        }
        validateInd(deleteIndFrom)

        val deleteIndTo = deleteIndFrom + length
        var curPieceLastInd = 0
        val iter = nodes.listIterator()
        while (deleteIndTo > curPieceLastInd && iter.hasNext()) {
            val curPiece = iter.next()
            val curPieceFirstInd = curPieceLastInd
            curPieceLastInd += curPiece.length
            if (curPieceLastInd < deleteIndFrom) {
                continue
            }
            iter.remove()

            val splitNode1 =
                Node(curPiece.type, curPiece.start, curPiece.length - curPieceLastInd + deleteIndFrom)
            val splitNode2 =
                Node(curPiece.type, curPiece.start + deleteIndTo - curPieceFirstInd, curPieceLastInd - deleteIndTo)
            if (splitNode1.length > 0) {
                iter.add(splitNode1)
            }
            if (splitNode2.length > 0) {
                iter.add(splitNode2)
            }
        }
        fixVersion()
    }

    private fun fixVersion() {
        version.set(version.incrementAndGet() % versions.size)
        versions[version.get()] = VersionedState(lastVersion.incrementAndGet(), LinkedList(nodes))
    }

    private fun validateInd(ind: Int) {
        if (ind < 0) {
            throw IllegalArgumentException("Ind should not be < 0 (actual $ind)")
        }
    }

    fun fullText(): String {
        val text = StringBuilder(original.length + added.length)
        for (node in nodes) {
            when (node.type) {
                NodeType.ORIGINAL -> {
                    for (i in 0 until node.length) {
                        text.append(original[node.start + i])
                    }
                }

                NodeType.ADDED -> {
                    for (i in 0 until node.length) {
                        text.append(added[node.start + i])
                    }
                }
            }

        }
        return text.toString()
    }

    companion object {
        @Suppress("unused")
        fun buildFromFile(f: File, e: Charset, undoDepth: Int = EngineSettings.defaultUndoDepth): InMemoryPieceTable {
            return try {
                val content = String(f.readBytes(), e)
                buildFromString(content, undoDepth)
            } catch (ex: IOException) {
                ex.printStackTrace()
                empty()
            }
        }

        fun buildFromString(content: String, undoDepth: Int = EngineSettings.defaultUndoDepth): InMemoryPieceTable {
            return if (content.isNotEmpty()) {
                InMemoryPieceTable(content, StringBuilder(""), undoDepth)
            } else {
                empty(undoDepth)
            }
        }

        private fun empty(undoDepth: Int = EngineSettings.defaultUndoDepth) =
            InMemoryPieceTable("", StringBuilder(""), undoDepth)

        private fun rootNode(s: String) = Node(NodeType.ORIGINAL, 0, s.length)

        @Suppress("unused")
        private fun lineStarts(s: String): List<Int> {
            var ind = -1
            val result = ArrayList<Int>()
            do {
                ind = s.indexOf(EngineSettings.lineSeparator, ind, false)
                result.add(ind)
            } while (ind >= 0)
            return result
        }
    }
}

class VersionedState(
    val version: Long,
    val state: LinkedList<Node>
)

class Node(
    val type: NodeType,
    val start: Int,
    val length: Int
)

enum class NodeType {
    ORIGINAL,
    ADDED
}
