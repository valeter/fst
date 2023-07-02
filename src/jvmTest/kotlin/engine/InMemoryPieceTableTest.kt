package engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author valter
 */
class InMemoryPieceTableTest {

    @Nested
    inner class MixedScenario {

        @Test
        fun multipleInsertsAndUndoAndDelete() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())

            table.insert("a", 0)
            assertEquals("aabc", table.fullText())

            table.insert("c", 4)
            assertEquals("aabcc", table.fullText())

            table.insert("ab", 2)
            assertEquals("aaabbcc", table.fullText())

            table.insert("bc", 5)
            assertEquals("aaabbbccc", table.fullText())

            table.undo()
            assertEquals("aaabbcc", table.fullText())

            table.delete(0, 7)
            assertEquals("", table.fullText())
        }

        @Test
        fun multipleDeletesAndUndoAndInsert() {
            val table = InMemoryPieceTable.buildFromString("aaabbbccc")
            table.delete(0, 1)
            assertEquals("aabbbccc", table.fullText())

            table.delete(1, 2)
            assertEquals("abbccc", table.fullText())

            table.delete(4, 2)
            assertEquals("abbc", table.fullText())

            table.delete(2, 2)
            assertEquals("ab", table.fullText())

            table.undo()
            assertEquals("abbc", table.fullText())

            table.insert("abc", 2)
            assertEquals("ababcbc", table.fullText())
        }

        @Test
        fun multipleInsertsAndDelete() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())

            table.insert("a", 0)
            assertEquals("aabc", table.fullText())

            table.insert("c", 4)
            assertEquals("aabcc", table.fullText())

            table.insert("ab", 2)
            assertEquals("aaabbcc", table.fullText())

            table.insert("bc", 5)
            assertEquals("aaabbbccc", table.fullText())

            table.delete(1, 7)
            assertEquals("ac", table.fullText())
        }

        @Test
        fun multipleDeletesAndInsert() {
            val table = InMemoryPieceTable.buildFromString("aaabbbccc")
            table.delete(0, 1)
            assertEquals("aabbbccc", table.fullText())

            table.delete(1, 2)
            assertEquals("abbccc", table.fullText())

            table.delete(4, 2)
            assertEquals("abbc", table.fullText())

            table.delete(2, 2)
            assertEquals("ab", table.fullText())

            table.insert("abc", 2)
            assertEquals("ababc", table.fullText())
        }

    }

    @Nested
    inner class Redo {

        @Test
        fun redoFromStart() {
            val table = InMemoryPieceTable.buildFromString("")
            table.redo()
            assertEquals("", table.fullText())
        }

        @Test
        fun redoFromStartAfterUndo() {
            val table = InMemoryPieceTable.buildFromString("")
            table.undo()
            assertEquals("", table.fullText())
            table.redo()
            assertEquals("", table.fullText())
        }

        @Test
        fun redoToInsert() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("", table.fullText())
            table.redo()
            assertEquals("abc", table.fullText())
        }

        @Test
        fun redoToDelete() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(0, 3)
            assertEquals("", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())
            table.redo()
            assertEquals("", table.fullText())
        }

        @Test
        fun multipleRedo() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())
            table.delete(0, 2)
            assertEquals("c", table.fullText())
            table.insert("ab", 1)
            assertEquals("cab", table.fullText())

            val expected: Array<String> = arrayOf("", "abc", "c", "cab")
            for (j in 0..2) {
                for (i in 1 until expected.size + 2) {
                    table.undo()
                    assertEquals(expected[0.coerceAtLeast(expected.size - i - 1)], table.fullText())
                }

                for (i in 1 until expected.size + 2) {
                    table.redo()
                    assertEquals(expected[i.coerceAtMost(expected.size - 1)], table.fullText())
                }
            }
        }

        @Test
        fun redoBranch() {
            // 0 = ""
            // 1 = abc
            // 2 = c
            // 3 = cab
            // 4 = cb
            // 5 = abcc
            // 6 = ab
            // 0 - 1 - 2 - 3 - 4
            //          \
            //           5 - 6
            // [0, 1, 2, 3, 4, 5, 6*]
            // [0, 1, 2, 3, 4*, 5, 6]
            // [0, 1, 2, 3, 4, 7*, 6]
            // [9*, 1, 2, 3, 4, 7, 8]

            // 0
            val table = InMemoryPieceTable.buildFromString("", 3)

            // 1
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())

            // 2
            table.delete(0, 2)
            assertEquals("c", table.fullText())

            // 3
            table.insert("ab", 1)
            assertEquals("cab", table.fullText())

            // 4
            // undoDepth = 3, 0 state is lost here
            table.delete(1, 1)
            assertEquals("cb", table.fullText())

            // 3
            table.undo()
            assertEquals("cab", table.fullText())

            // 2
            table.undo()
            assertEquals("c", table.fullText())

            // 5
            table.insert("abc", 0)
            assertEquals("abcc", table.fullText())

            // 6
            table.delete(2, 2)
            assertEquals("ab", table.fullText())

            // 5
            table.undo()
            assertEquals("abcc", table.fullText())

            // 2
            table.undo()
            assertEquals("c", table.fullText())

            // 1
            table.undo()
            assertEquals("abc", table.fullText())

            // still 1
            // undoDepth = 3, 0 state is lost
            table.undo()
            assertEquals("abc", table.fullText())

            // 2
            table.redo()
            assertEquals("c", table.fullText())

            // 5
            table.redo()
            assertEquals("abcc", table.fullText())

            // 6
            table.redo()
            assertEquals("ab", table.fullText())
        }

        @Test
        fun redoDepth() {
            val table = InMemoryPieceTable.buildFromString("", 1)
            table.insert("c", 0)
            assertEquals("c", table.fullText())
            table.insert("b", 0)
            assertEquals("bc", table.fullText())
            table.insert("a", 0)
            assertEquals("abc", table.fullText())
            table.insert("d", 0)
            assertEquals("dabc", table.fullText())

            table.undo()
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())

            table.redo()
            assertEquals("dabc", table.fullText())
            table.redo()
            assertEquals("dabc", table.fullText())
        }

    }

    @Nested
    inner class Undo {

        @Test
        fun undoFromStart() {
            val table = InMemoryPieceTable.buildFromString("")
            table.undo()
            assertEquals("", table.fullText())
        }

        @Test
        fun undoFromInsert() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("", table.fullText())
        }

        @Test
        fun undoFromDelete() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(0, 3)
            assertEquals("", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())
        }

        @Test
        fun multipleUndo() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())
            table.delete(0, 2)
            assertEquals("c", table.fullText())
            table.insert("ab", 1)
            assertEquals("cab", table.fullText())

            table.undo()
            assertEquals("c", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("", table.fullText())
            table.undo()
            assertEquals("", table.fullText())
        }

        @Test
        fun undoBranch() {
            // 0 = ""
            // 1 = abc
            // 2 = c
            // 3 = cab
            // 4 = cb
            // 5 = abcc
            // 6 = ab
            // 0 - 1 - 2 - 3 - 4
            //          \
            //           5 - 6
            // [0, 1, 2, 3, 4, 5, 6*]
            // [0, 1, 2, 3, 4*, 5, 6]
            // [0, 1, 2, 3, 4, 7*, 6]
            // [9*, 1, 2, 3, 4, 7, 8]

            // 0
            val table = InMemoryPieceTable.buildFromString("", 3)

            // 1
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())

            // 2
            table.delete(0, 2)
            assertEquals("c", table.fullText())

            // 3
            table.insert("ab", 1)
            assertEquals("cab", table.fullText())

            // 4
            // depth = 3, 0 state is lost here
            table.delete(1, 1)
            assertEquals("cb", table.fullText())

            // 3
            table.undo()
            assertEquals("cab", table.fullText())

            // 2
            table.undo()
            assertEquals("c", table.fullText())

            // 5
            table.insert("abc", 0)
            assertEquals("abcc", table.fullText())

            // 6
            table.delete(2, 2)
            assertEquals("ab", table.fullText())

            table.undo()
            assertEquals("abcc", table.fullText())
            table.undo()
            assertEquals("c", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())

            // depth = 3, 0 state is lost
            table.undo()
            assertEquals("abc", table.fullText())
        }

        @Test
        fun undoDepth() {
            val table = InMemoryPieceTable.buildFromString("", 1)
            table.insert("c", 0)
            assertEquals("c", table.fullText())
            table.insert("b", 0)
            assertEquals("bc", table.fullText())
            table.insert("a", 0)
            assertEquals("abc", table.fullText())
            table.insert("d", 0)
            assertEquals("dabc", table.fullText())

            table.undo()
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())
            table.undo()
            assertEquals("abc", table.fullText())
        }


    }

    @Nested
    inner class Delete {

        @Test
        fun deleteNoneFromEmptyTable() {
            val table = InMemoryPieceTable.buildFromString("")
            table.delete(0, 0)
            assertEquals("", table.fullText())
        }

        @Test
        fun deleteSymbolFromEmptyTable() {
            val table = InMemoryPieceTable.buildFromString("")
            table.delete(0, 1)
            assertEquals("", table.fullText())
        }

        @Test
        fun deleteNoneFromText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(0, 0)
            assertEquals("abc", table.fullText())
        }

        @Test
        fun deleteFullText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(0, 3)
            assertEquals("", table.fullText())
        }

        @Test
        fun deleteFirstSymbolFromText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(0, 1)
            assertEquals("bc", table.fullText())
        }

        @Test
        fun deleteLastSymbolFromText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(2, 1)
            assertEquals("ab", table.fullText())
        }

        @Test
        fun deleteMiddleSymbolFromText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.delete(1, 1)
            assertEquals("ac", table.fullText())
        }

        @Test
        fun deleteMiddleTextFromText() {
            val table = InMemoryPieceTable.buildFromString("abcd")
            table.delete(1, 2)
            assertEquals("ad", table.fullText())
        }

    }

    @Nested
    inner class Insert {
        @Test
        fun insertTextToEmptyTable() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("abc", 0)
            assertEquals("abc", table.fullText())
        }

        @Test
        fun insertNothingEmptyTable() {
            val table = InMemoryPieceTable.buildFromString("")
            table.insert("", 0)
            assertEquals("", table.fullText())
        }

        @Test
        fun insertSymbolToStartOfTheText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.insert("d", 0)
            assertEquals("dabc", table.fullText())
        }

        @Test
        fun insertSymbolToEndOfTheText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.insert("d", 3)
            assertEquals("abcd", table.fullText())
        }

        @Test
        fun insertSymbolToMiddleOfTheText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.insert("d", 2)
            assertEquals("abdc", table.fullText())
        }

        @Test
        fun insertTextToMiddleOfTheText() {
            val table = InMemoryPieceTable.buildFromString("abc")
            table.insert("abc", 1)
            assertEquals("aabcbc", table.fullText())
        }
    }

}
