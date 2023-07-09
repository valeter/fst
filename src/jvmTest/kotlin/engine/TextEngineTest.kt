package engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * @author valter
 */
class TextEngineTest {

    companion object {
        @JvmStatic
        fun getEngines(): List<Arguments> {
            return getEngineBuilders()
                .map { builder ->
                    @Suppress("UNCHECKED_CAST")
                    (builder.get()[0] as Function1<Int, TextEngine>).invoke(EngineSettings.defaultUndoDepth)
                }
                .map { Arguments.of(it) }
                .toList()
        }

        @JvmStatic
        fun getEngineBuilders(): List<Arguments> {
            return listOf(
                Arguments.of({ undoDepth: Int -> TextEngine.empty(TextEngineType.IN_MEMORY_STRING, undoDepth) }),
                Arguments.of({ undoDepth: Int -> TextEngine.empty(TextEngineType.IN_MEMORY_PIECE_TABLE, undoDepth) }),
            )
        }
    }

    @Nested
    inner class MixedScenario {

        @EngineTest
        fun multipleInsertsAndUndoAndDelete(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())

            engine.insert("a", 0)
            assertEquals("aabc", engine.fullText())

            engine.insert("c", 4)
            assertEquals("aabcc", engine.fullText())

            engine.insert("ab", 2)
            assertEquals("aaabbcc", engine.fullText())

            engine.insert("bc", 5)
            assertEquals("aaabbbccc", engine.fullText())

            engine.undo()
            assertEquals("aaabbcc", engine.fullText())

            engine.delete(0, 7)
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun multipleDeletesAndUndoAndInsert(engine: TextEngine) {
            engine.insert("aaabbbccc", 0)
            engine.delete(0, 1)
            assertEquals("aabbbccc", engine.fullText())

            engine.delete(1, 2)
            assertEquals("abbccc", engine.fullText())

            engine.delete(4, 2)
            assertEquals("abbc", engine.fullText())

            engine.delete(2, 2)
            assertEquals("ab", engine.fullText())

            engine.undo()
            assertEquals("abbc", engine.fullText())

            engine.insert("abc", 2)
            assertEquals("ababcbc", engine.fullText())
        }

        @EngineTest
        fun multipleInsertsAndDelete(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())

            engine.insert("a", 0)
            assertEquals("aabc", engine.fullText())

            engine.insert("c", 4)
            assertEquals("aabcc", engine.fullText())

            engine.insert("ab", 2)
            assertEquals("aaabbcc", engine.fullText())

            engine.insert("bc", 5)
            assertEquals("aaabbbccc", engine.fullText())

            engine.delete(1, 7)
            assertEquals("ac", engine.fullText())
        }

        @EngineTest
        fun multipleDeletesAndInsert(engine: TextEngine) {
            engine.insert("aaabbbccc", 0)
            engine.delete(0, 1)
            assertEquals("aabbbccc", engine.fullText())

            engine.delete(1, 2)
            assertEquals("abbccc", engine.fullText())

            engine.delete(4, 2)
            assertEquals("abbc", engine.fullText())

            engine.delete(2, 2)
            assertEquals("ab", engine.fullText())

            engine.insert("abc", 2)
            assertEquals("ababc", engine.fullText())
        }

    }

    @Nested
    inner class Redo {

        @EngineTest
        fun redoFromStart(engine: TextEngine) {
            engine.redo()
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun redoFromStartAfterUndo(engine: TextEngine) {
            engine.undo()
            assertEquals("", engine.fullText())
            engine.redo()
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun redoToInsert(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("", engine.fullText())
            engine.redo()
            assertEquals("abc", engine.fullText())
        }

        @EngineTest
        fun redoToDelete(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(0, 3)
            assertEquals("", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())
            engine.redo()
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun multipleRedo(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())
            engine.delete(0, 2)
            assertEquals("c", engine.fullText())
            engine.insert("ab", 1)
            assertEquals("cab", engine.fullText())

            val expected: Array<String> = arrayOf("", "abc", "c", "cab")
            for (j in 0..2) {
                for (i in 1 until expected.size + 2) {
                    engine.undo()
                    assertEquals(expected[0.coerceAtLeast(expected.size - i - 1)], engine.fullText())
                }

                for (i in 1 until expected.size + 2) {
                    engine.redo()
                    assertEquals(expected[i.coerceAtMost(expected.size - 1)], engine.fullText())
                }
            }
        }

        @EngineBuilderTest
        fun redoBranch(builder: Function1<Int, TextEngine>) {
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

            val engine = builder.invoke(3)

            // 1
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())

            // 2
            engine.delete(0, 2)
            assertEquals("c", engine.fullText())

            // 3
            engine.insert("ab", 1)
            assertEquals("cab", engine.fullText())

            // 4
            // undoDepth = 3, 0 state is lost here
            engine.delete(1, 1)
            assertEquals("cb", engine.fullText())

            // 3
            engine.undo()
            assertEquals("cab", engine.fullText())

            // 2
            engine.undo()
            assertEquals("c", engine.fullText())

            // 5
            engine.insert("abc", 0)
            assertEquals("abcc", engine.fullText())

            // 6
            engine.delete(2, 2)
            assertEquals("ab", engine.fullText())

            // 5
            engine.undo()
            assertEquals("abcc", engine.fullText())

            // 2
            engine.undo()
            assertEquals("c", engine.fullText())

            // 1
            engine.undo()
            assertEquals("abc", engine.fullText())

            // still 1
            // undoDepth = 3, 0 state is lost
            engine.undo()
            assertEquals("abc", engine.fullText())

            // 2
            engine.redo()
            assertEquals("c", engine.fullText())

            // 5
            engine.redo()
            assertEquals("abcc", engine.fullText())

            // 6
            engine.redo()
            assertEquals("ab", engine.fullText())
        }

        @EngineBuilderTest
        fun redoDepth(builder: Function1<Int, TextEngine>) {
            val engine = builder.invoke(1)
            engine.insert("c", 0)
            assertEquals("c", engine.fullText())
            engine.insert("b", 0)
            assertEquals("bc", engine.fullText())
            engine.insert("a", 0)
            assertEquals("abc", engine.fullText())
            engine.insert("d", 0)
            assertEquals("dabc", engine.fullText())

            engine.undo()
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())

            engine.redo()
            assertEquals("dabc", engine.fullText())
            engine.redo()
            assertEquals("dabc", engine.fullText())
        }

    }

    @Nested
    inner class Undo {

        @EngineTest
        fun undoFromStart(engine: TextEngine) {
            engine.undo()
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun undoFromInsert(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun undoFromDelete(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(0, 3)
            assertEquals("", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())
        }

        @EngineTest
        fun multipleUndo(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())
            engine.delete(0, 2)
            assertEquals("c", engine.fullText())
            engine.insert("ab", 1)
            assertEquals("cab", engine.fullText())

            engine.undo()
            assertEquals("c", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("", engine.fullText())
            engine.undo()
            assertEquals("", engine.fullText())
        }

        @EngineBuilderTest
        fun undoBranch(builder: Function1<Int, TextEngine>) {
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
            val engine = builder.invoke(3)

            // 1
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())

            // 2
            engine.delete(0, 2)
            assertEquals("c", engine.fullText())

            // 3
            engine.insert("ab", 1)
            assertEquals("cab", engine.fullText())

            // 4
            // depth = 3, 0 state is lost here
            engine.delete(1, 1)
            assertEquals("cb", engine.fullText())

            // 3
            engine.undo()
            assertEquals("cab", engine.fullText())

            // 2
            engine.undo()
            assertEquals("c", engine.fullText())

            // 5
            engine.insert("abc", 0)
            assertEquals("abcc", engine.fullText())

            // 6
            engine.delete(2, 2)
            assertEquals("ab", engine.fullText())

            engine.undo()
            assertEquals("abcc", engine.fullText())
            engine.undo()
            assertEquals("c", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())

            // depth = 3, 0 state is lost
            engine.undo()
            assertEquals("abc", engine.fullText())
        }

        @EngineBuilderTest
        fun undoDepth(builder: Function1<Int, TextEngine>) {
            val engine = builder.invoke(1)
            engine.insert("c", 0)
            assertEquals("c", engine.fullText())
            engine.insert("b", 0)
            assertEquals("bc", engine.fullText())
            engine.insert("a", 0)
            assertEquals("abc", engine.fullText())
            engine.insert("d", 0)
            assertEquals("dabc", engine.fullText())

            engine.undo()
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())
            engine.undo()
            assertEquals("abc", engine.fullText())
        }

    }

    @Nested
    inner class Delete {

        @EngineTest
        fun deleteNoneFromEmptyTable(engine: TextEngine) {
            engine.delete(0, 0)
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun deleteSymbolFromEmptyTable(engine: TextEngine) {
            engine.delete(0, 1)
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun deleteNoneFromText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(0, 0)
            assertEquals("abc", engine.fullText())
        }

        @EngineTest
        fun deleteFullText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(0, 3)
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun deleteFirstSymbolFromText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(0, 1)
            assertEquals("bc", engine.fullText())
        }

        @EngineTest
        fun deleteLastSymbolFromText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(2, 1)
            assertEquals("ab", engine.fullText())
        }

        @EngineTest
        fun deleteMiddleSymbolFromText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.delete(1, 1)
            assertEquals("ac", engine.fullText())
        }

        @EngineTest
        fun deleteMiddleTextFromText(engine: TextEngine) {
            engine.insert("abcd", 0)
            engine.delete(1, 2)
            assertEquals("ad", engine.fullText())
        }

    }

    @Nested
    inner class Insert {
        @EngineTest
        fun insertTextToEmptyTable(engine: TextEngine) {
            engine.insert("abc", 0)
            assertEquals("abc", engine.fullText())
        }

        @EngineTest
        fun insertNothingEmptyTable(engine: TextEngine) {
            engine.insert("", 0)
            assertEquals("", engine.fullText())
        }

        @EngineTest
        fun insertSymbolToStartOfTheText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.insert("d", 0)
            assertEquals("dabc", engine.fullText())
        }

        @EngineTest
        fun insertSymbolToEndOfTheText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.insert("d", 3)
            assertEquals("abcd", engine.fullText())
        }

        @EngineTest
        fun insertSymbolToMiddleOfTheText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.insert("d", 2)
            assertEquals("abdc", engine.fullText())
        }

        @EngineTest
        fun insertTextToMiddleOfTheText(engine: TextEngine) {
            engine.insert("abc", 0)
            engine.insert("abc", 1)
            assertEquals("aabcbc", engine.fullText())
        }
    }

}


@ParameterizedTest(name = "{displayName} engine={0}")
@MethodSource("engine.TextEngineTest#getEngines")
annotation class EngineTest {

}

@ParameterizedTest(name = "{displayName} engine={0}")
@MethodSource("engine.TextEngineTest#getEngineBuilders")
annotation class EngineBuilderTest {

}
