package moe.nea.cittofirm.studio

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.util.StringConverter
import tornadofx.CssBox
import tornadofx.MultiValue
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.combobox
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
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

sealed interface GenericModel : ResourcePackFile
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

class CustomItemModelEditor(
	val model: CustomItemModel,
	val json: JsonObject,
) : UIComponent(model.skyblockItemId) {
	override val root = vbox {
		form {
			fieldset("Rendering") {
			}
			fieldset("Override") {
				field("Head Model Override") {
					combobox<ResourcePackFile> {
						val sentinelNull = ProjectPath.of(Identifier("cittofirminternal", "models/item/null_model"))
							.intoFile()!!
						// TODO: make my own TransformationList that prepends the null
						itemsProperty().set((
							FXCollections.concat(FXCollections.singletonObservableList(sentinelNull),
							                     project.models)))
						converter = object : StringConverter<ResourcePackFile?>() {
							override fun toString(p0: ResourcePackFile?): String {
								if (p0 == sentinelNull) return "No custom model."
								return p0?.file?.identifier?.toString() ?: ""
							}

							override fun fromString(p0: String?): ResourcePackFile {
								TODO("Not yet implemented")
							}
						}
						valueProperty().set(json["firmament:head_model"]
							                    ?.asString
							                    ?.let(Identifier::parse)
							                    ?.let(ProjectPath::of)
							                    ?.intoFile() ?: sentinelNull)
						valueProperty().addListener { obv, o, n ->
							json.addProperty("firmament:head_model", (if (n == sentinelNull) null else n)
								?.file?.identifier?.toString())
						}
						// TODO: add create model button
					}
				}
			}
			button("Save") {
				action {
					model.saveNewJson(project.resourcePackBase, json)
				}
			}
		}
	}
}

val UIComponent.project get() = workspace as ProjectWindow

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