@file:Suppress("NewApi")

package platform


actual val HomeFolder: File get() = java.io.File(System.getProperty("user.home")).toProjectFile()
