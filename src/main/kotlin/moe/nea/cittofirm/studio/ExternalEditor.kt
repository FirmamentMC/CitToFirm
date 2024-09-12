package moe.nea.cittofirm.studio

import com.google.auto.service.AutoService
import moe.nea.cittofirm.studio.util.CommandUtils
import java.util.ServiceLoader
import kotlin.io.path.absolutePathString

interface ExternalEditor {
	fun canOpen(resourcePackFile: ResourcePackFile): Boolean
	val name: String
	val isAvailable: Boolean
	fun open(projectWindow: ProjectWindow, resourcePackFile: ResourcePackFile)

	companion object {
		// TODO: allow manually disabling editors in the settings
		val editors = ServiceLoader.load(ExternalEditor::class.java)
			.filter { it.isAvailable }
	}
}

@AutoService(ExternalEditor::class)
class VSCode : ExternalEditor {
	override fun canOpen(resourcePackFile: ResourcePackFile): Boolean {
		return resourcePackFile.file.extension in listOf("json", "mcmeta", "lang", "txt")
	}

	override val name: String
		get() = "VSCode"
	override val isAvailable: Boolean
		get() = CommandUtils.executableExists("code")

	override fun open(projectWindow: ProjectWindow, resourcePackFile: ResourcePackFile) {
		Runtime.getRuntime().exec(arrayOf("code", projectWindow.getRealPath(resourcePackFile).absolutePathString()))
	}
}

@AutoService(ExternalEditor::class)
class Gimp : ExternalEditor {
	override val isAvailable = CommandUtils.executableExists("gimp")

	override fun canOpen(resourcePackFile: ResourcePackFile): Boolean {
		return resourcePackFile is ImageFile
	}

	override val name: String
		get() = "GIMP"

	override fun open(projectWindow: ProjectWindow, resourcePackFile: ResourcePackFile) {
		resourcePackFile as ImageFile
		Runtime.getRuntime().exec(arrayOf("gimp", projectWindow.getRealPath(resourcePackFile).absolutePathString()))
	}
}