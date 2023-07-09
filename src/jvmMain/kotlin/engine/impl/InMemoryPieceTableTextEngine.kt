package engine.impl

import engine.EngineSettings
import engine.TextEngine
import engine.Versions
import java.util.*

/**
 * @author valter
 */
class InMemoryPieceTableTextEngine(
    private val original: String,
    private val added: StringBuilder,
    undoDepth: Int = EngineSettings.defaultUndoDepth
) : TextEngine {

    private var nodes: LinkedList<Node> =
        if (original.isEmpty()) LinkedList() else LinkedList(listOf(rootNode(original)))

    private val versions = Versions(version(), undoDepth + 1)

    override fun undo() {
        versions.switchToPrevVersion()?.let {
            nodes = LinkedList(it)
        }
    }

    override fun redo() {
        versions.switchToNextVersion()?.let {
            nodes = LinkedList(it)
        }
    }

    override fun insert(s: String, insertInd: Int) {
        validateInsert(s, insertInd)

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
        versions.addVersion(version())
    }

    override fun delete(deleteIndFrom: Int, length: Int) {
        validateDelete(deleteIndFrom, length)

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
        versions.addVersion(version())
    }

    private fun version(): LinkedList<Node> = LinkedList(nodes)

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    override fun fullText(): String {
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

        private fun rootNode(s: String) = Node(NodeType.ORIGINAL, 0, s.length)

    }

    class Node(
        val type: NodeType,
        val start: Int,
        val length: Int
    )

    enum class NodeType {
        ORIGINAL,
        ADDED
    }

}

