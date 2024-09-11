package moe.nea.cittofirm.studio

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import moe.nea.cittofirm.studio.util.observedCheapMap
import moe.nea.cittofirm.studio.util.observedFilter
import moe.nea.cittofirm.studio.util.stringProperty
import moe.nea.cittofirm.studio.util.withPrepended
import tornadofx.CssBox
import tornadofx.MultiValue
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hbox
import tornadofx.label
import tornadofx.px
import tornadofx.style
import tornadofx.text
import tornadofx.tooltip
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

val sentinelNull = ProjectPath.of(Identifier("cittofirminternal", "models/item/null_model"))
	.intoFile() as GenericModel

class CustomItemModelEditor(
	val model: CustomItemModel,
	val json: JsonObject,
) : UIComponent(model.skyblockItemId) {
	val parentProp = json.stringProperty("parent")
	val headModelProp = json.stringProperty("firmament:head_model")
	override val root = vbox {
		form {
			fieldset("Rendering") {
				field("Parent Model") {
					tooltip("The parent model supplies fallbacks for every option not specified. Typically this is used with a model shape that has its textures overridden in the child model.")
					autoCompletableTextField {
						searchFunction = { search ->
							project.models.withPrepended(sentinelNull)
								.observedFilter { Identifier.search(search, it.file.identifier!!) }
								.observedCheapMap {
									if (it == sentinelNull) ""
									else it.modelIdentifier.toString()
								}
						}
						textProperty().bindBidirectional(parentProp)
					}
				}
			}
			fieldset("Override") {
				label("Predicate Overrides only get applied once. These options only matter if this is the first model loaded. The Head Model Override gets applied according to the resolved model.") {
					isWrapText = true
				}
				field("Head Model Override") {
					tooltip("Override how this item renders when equipped as a helmet.\nThis needs to point to another model.")
					autoCompletableTextField {
						searchFunction = { search ->
							project.models.withPrepended(sentinelNull)
								.observedFilter { Identifier.search(search, it.file.identifier!!) }
								.observedCheapMap {
									if (it == sentinelNull) ""
									else it.modelIdentifier.toString()
								}
						}
						textProperty().bindBidirectional(headModelProp)
					}
					// TODO: error indicator here? maybe a link if valid / error button if not
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
	override fun openUI(basePath: Path): UIComponent {
		return ErrorEditor(file.identifier.toString(), file.resolve(basePath))
	}
}