package moe.nea.cittofirm.studio

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import tornadofx.CssBox
import tornadofx.MultiValue
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.hbox
import tornadofx.label
import tornadofx.px
import tornadofx.style
import tornadofx.text
import tornadofx.vbox
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

sealed interface ResourcePackFile : Comparable<ResourcePackFile> {
	val file: ProjectPath
	override fun compareTo(other: ResourcePackFile): Int {
		return file.compareTo(other.file)
	}

	fun openUI(basePath: Path): UIComponent
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


class ErrorEditor(name: String, val file: Path) : UIComponent("Error - $name") {
	override val root = vbox(alignment = Pos.CENTER) {
		hbox {
			alignment = Pos.CENTER
			vbox(alignment = Pos.CENTER) {
				padding = Insets(3.0)
				style {
					backgroundColor = MultiValue(arrayOf(Color.RED.interpolate(Color.TRANSPARENT, 0.4)))
					backgroundRadius = MultiValue(arrayOf(CssBox(15.px, 15.px, 15.px, 15.px)))
				}
				label("Error!") {
					textAlignment = TextAlignment.CENTER
					style {
						fontSize = 30.px
					}
				}
				text("Could not edit file $name. Either loading this file caused an error, or this file cannot be edited in FirmStudio.") {
					wrappingWidthProperty().set(300.0)
				}
				button("Open externally") {
					action {
						hostServices.showDocument(file.toUri().toString())
					}
				}
				autosize()
			}
		}
	}
}

data class ImageFile(
	override val file: ProjectPath
) : ResourcePackFile {
	val textureIdentifier get() = file.identifier!!.withoutKnownPath(KnownPath.genericTexture)

	override fun openUI(basePath: Path): UIComponent {
		return ErrorEditor(file.identifier.toString(), file.resolve(basePath))
	}
}