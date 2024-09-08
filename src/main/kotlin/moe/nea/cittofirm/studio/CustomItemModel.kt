package moe.nea.cittofirm.studio

import com.google.gson.Gson
import com.google.gson.JsonObject
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import tornadofx.CssBox
import tornadofx.MultiValue
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.label
import tornadofx.px
import tornadofx.style
import tornadofx.text
import tornadofx.vbox
import java.nio.file.Path
import kotlin.io.path.readText

sealed interface ResourcePackFile : Comparable<ResourcePackFile> {
	val file: ProjectPath
	override fun compareTo(other: ResourcePackFile): Int {
		return file.compareTo(other.file)
	}

	fun openUI(basePath: Path): UIComponent
}

val gson = Gson()

data class CustomItemModel(
	val skyblockItemId: String,
	override val file: ProjectPath,
) : ResourcePackFile {
	override fun openUI(basePath: Path): UIComponent {
		val json = gson.fromJson(file.resolve(basePath).readText(), JsonObject::class.java)
		return CustomItemModelEditor(this, json)
	}
}

class CustomItemModelEditor(
	val model: CustomItemModel,
	val json: JsonObject,
) : UIComponent(model.skyblockItemId) {
	override val root = label("Coming soon!") { }
}

class ErrorEditor(name: String, val file: Path) : UIComponent("Error - $name") {
	override val root = vbox {
		alignment = Pos.TOP_CENTER
		vbox(alignment = Pos.CENTER) {
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

data class ImageFile(
	override val file: ProjectPath
) : ResourcePackFile {
	override fun openUI(basePath: Path): UIComponent {
		return ErrorEditor(file.identifier.toString(), file.resolve(basePath))
	}
}