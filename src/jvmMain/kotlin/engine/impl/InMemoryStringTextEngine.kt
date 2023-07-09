package engine.impl

import engine.EngineSettings
import engine.TextEngine
import engine.Versions

/**
 * @author valter
 */
class InMemoryStringTextEngine(
    private var text: String,
    undoDepth: Int = EngineSettings.defaultUndoDepth
) : TextEngine {

    private val versions = Versions<Action>(ActionEmpty(), undoDepth + 1)

    override fun undo() {
        if (versions.canSwitchToPrevVersion()) {
            versions.curVersion()?.let {
                text = it.undo(text)
            }
            versions.switchToPrevVersion()
        }
    }

    override fun redo() {
        if (versions.canSwitchToNextVersion()) {
            versions.switchToNextVersion()?.let {
                text = it.redo(text)
            }
        }
    }

    override fun insert(s: String, insertInd: Int) {
        validateInsert(s, insertInd)
        text = insertResult(text, s, insertInd)
        versions.addVersion(ActionInsert(insertInd, s))
    }

    override fun delete(deleteIndFrom: Int, length: Int) {
        validateDelete(deleteIndFrom, length)
        val actualInd = minOf(deleteIndFrom, text.length)
        val actualLength = minOf(length, text.length - actualInd)
        val deletedFragment = text.substring(actualInd, actualInd + actualLength)
        text = deleteResult(text, deleteIndFrom, length)
        versions.addVersion(ActionDelete(deleteIndFrom, deletedFragment))
    }

    override fun fullText() = text

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    companion object {
        private fun insertResult(currentText: String, s: String, insertInd: Int): String {
            val actualInd = minOf(insertInd, currentText.length)
            val res = StringBuilder(currentText.substring(0, actualInd))
            res.append(s)
            res.append(currentText.substring(actualInd, currentText.length))
            return res.toString()
        }

        private fun deleteResult(currentText: String, deleteIndFrom: Int, length: Int): String {
            val actualInd = minOf(deleteIndFrom, currentText.length)
            val actualLength = minOf(length, currentText.length - actualInd)
            val res = StringBuilder(currentText.substring(0, actualInd))
            res.append(currentText.substring(actualInd + actualLength, currentText.length))
            return res.toString()
        }
    }

    abstract inner class Action {
        abstract fun undo(current: String): String

        abstract fun redo(current: String): String
    }

    inner class ActionEmpty : Action() {
        override fun undo(current: String) = current

        override fun redo(current: String) = current
    }

    inner class ActionDelete(
        private val deleteIndFrom: Int,
        private val deletedFragment: String
    ) : Action() {
        override fun undo(current: String): String = insertResult(current, deletedFragment, deleteIndFrom)

        override fun redo(current: String): String = deleteResult(current, deleteIndFrom, deletedFragment.length)
    }

    inner class ActionInsert(
        private val insertInd: Int,
        private val insertedFragment: String
    ) : Action() {
        override fun undo(current: String): String = deleteResult(current, insertInd, insertedFragment.length)

        override fun redo(current: String): String = insertResult(current, insertedFragment, insertInd)
    }

}
