package engine

import engine.impl.InMemoryPieceTableTextEngine
import engine.impl.InMemoryStringTextEngine
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

/**
 * @author valter
 */
interface TextEngine {

    fun undo()

    fun redo()

    fun insert(s: String, insertInd: Int)

    fun delete(deleteIndFrom: Int, length: Int)

    fun fullText(): String

    fun validateInsert(s: String, insertInd: Int) {
        if (s.isEmpty()) {
            return
        }
        validateInd(insertInd)
    }

    fun validateDelete(deleteIndFrom: Int, length: Int) {
        if (length == 0) {
            return
        }
        if (length < 0) {
            throw IllegalArgumentException("Length of deleted text should not be < 0 (actual $length)")
        }
        validateInd(deleteIndFrom)
    }

    private fun validateInd(ind: Int) {
        if (ind < 0) {
            throw IllegalArgumentException("Ind should not be < 0 (actual $ind)")
        }
    }

    companion object {

        @Suppress("unused")
        fun buildFromFile(
            type: TextEngineType, f: File, e: Charset, undoDepth: Int = EngineSettings.defaultUndoDepth
        ): TextEngine {
            return try {
                val content = String(f.readBytes(), e)
                buildFromString(type, content, undoDepth)
            } catch (ex: IOException) {
                ex.printStackTrace()
                empty(type, undoDepth)
            }
        }

        fun buildFromString(
            type: TextEngineType,
            content: String,
            undoDepth: Int = EngineSettings.defaultUndoDepth
        ): TextEngine {
            return when (type) {
                TextEngineType.IN_MEMORY_STRING -> InMemoryStringTextEngine(content, undoDepth)
                TextEngineType.IN_MEMORY_PIECE_TABLE ->
                    InMemoryPieceTableTextEngine(content, StringBuilder(""), undoDepth)
            }
        }

        fun empty(type: TextEngineType, undoDepth: Int = EngineSettings.defaultUndoDepth) =
            buildFromString(type, "", undoDepth)

    }

}

enum class TextEngineType {
    IN_MEMORY_STRING,
    IN_MEMORY_PIECE_TABLE,
}
