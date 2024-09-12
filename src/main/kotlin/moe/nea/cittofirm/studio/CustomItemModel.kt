package moe.nea.cittofirm.studio

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import tornadofx.UIComponent
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

sealed interface ResourcePackFile : Comparable<ResourcePackFile> {
	val file: ProjectPath
	override fun compareTo(other: ResourcePackFile): Int {
		return file.compareTo(other.file)
	}

	fun openUI(basePath: Path): UIComponent

	fun lint(): List<LintError> = emptyList()
}

val gson = GsonBuilder().setPrettyPrinting().create()

sealed interface GenericModel : ResourcePackFile {
	val modelIdentifier get() = file.identifier!!.withoutKnownPath("models/", ".json")
}

data class NonCustomItemModel(
	override val file: ProjectPath
) : GenericModel {
	override fun openUI(basePath: Path): UIComponent {
		TODO("Not yet implemented")
	}
}

data class CustomItemModel(
	val skyblockItemId: String,
	override val file: ProjectPath,
) : GenericModel {
	override fun openUI(basePath: Path): UIComponent {
		val json = gson.fromJson(file.resolve(basePath).readText(), JsonObject::class.java)
		return CustomItemModelEditor(this, json)
	}

	fun saveNewJson(basePath: Path, json: JsonObject) {
		file.resolve(basePath).writeText(gson.toJson(json))
	}
}

data class ImageFile(
	override val file: ProjectPath
) : ResourcePackFile {
	val textureIdentifier get() = file.identifier!!.withoutKnownPath(KnownPath.genericTexture)

	override fun openUI(basePath: Path): UIComponent {
		return ImageEditor(this, file.resolve(basePath))
	}
}