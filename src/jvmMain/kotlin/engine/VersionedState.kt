package engine

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author valter
 */
class Versions<T>(
    initVersion: T,
    maxVersions: Int
) {
    private val version = AtomicInteger(0)
    private val lastVersion = AtomicLong(0L)

    private val versions: Array<VersionedState?> =
        Array(maxVersions) { i ->
            if (i == 0) VersionedState(
                0L,
                initVersion
            ) else null
        }

    fun addVersion(v: T) {
        version.set(version.incrementAndGet() % versions.size)
        versions[version.get()] =
            VersionedState(lastVersion.incrementAndGet(), v)
    }

    fun curVersion(): T? = versions[version.get()]?.state

    fun canSwitchToPrevVersion(): Boolean {
        val prevVersion = prevVersionInd()
        return prevVersion != version.get()
                && versions[prevVersion] != null
                // max undoDepth not reached
                && versions[prevVersion]!!.version <= versions[version.get()]!!.version
    }

    private fun prevVersionInd() = (version.get() + versions.size - 1) % versions.size


    // 0 - 1 - 2 - 3 - 4 - 5 - 6
    //                  \
    //                   7 - 8 - 9
    // [0, 1, 2, 3, 4, 5, 6*]
    // [0, 1, 2, 3, 4*, 5, 6]
    // [0, 1, 2, 3, 4, 7*, 6]
    // [9*, 1, 2, 3, 4, 7, 8]
    fun switchToPrevVersion(): T? {
        if (versions[version.get()] == null) {
            throw IllegalArgumentException("Unreachable statement")
        }
        if (!canSwitchToPrevVersion()) {
            return null
        }
        val prevVersion = prevVersionInd()
        version.set(prevVersion)
        return versions[prevVersion]!!.state
    }

    fun canSwitchToNextVersion(): Boolean {
        val nextVersion = nextVersionInd()
        return nextVersion != version.get()
                && versions[nextVersion] != null
                // top of undoDepth stack not reached
                && versions[nextVersion]!!.version >= versions[version.get()]!!.version
    }

    private fun nextVersionInd() = (version.get() + 1) % versions.size

    fun switchToNextVersion(): T? {
        if (versions[version.get()] == null) {
            throw IllegalArgumentException("Unreachable statement")
        }
        if (!canSwitchToNextVersion()) {
            return null
        }
        val nextVersion = nextVersionInd()
        version.set(nextVersion)
        return versions[nextVersion]!!.state
    }

    inner class VersionedState(
        val version: Long,
        val state: T
    ) {
        override fun toString(): String = "version=$version state=$state"
    }
}
