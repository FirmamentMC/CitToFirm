package moe.nea.cittofirm.studio.util

import java.io.File

object CommandUtils {
	val path = System.getenv("PATH").split(File.pathSeparator)
	val validExtensions = listOf("", "exe", "bat", "cmd")
	fun executableExists(binaryName: String): Boolean {
		return path.any { p ->
			val f = File(p)
			validExtensions.any { ext ->
				f.resolve(binaryName + ext).canExecute()
			}
		}
	}
}