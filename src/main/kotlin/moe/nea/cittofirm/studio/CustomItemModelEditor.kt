package moe.nea.cittofirm.studio

import com.google.gson.JsonObject
import javafx.beans.InvalidationListener
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import moe.nea.cittofirm.studio.util.observedCheapMap
import moe.nea.cittofirm.studio.util.observedFilter
import moe.nea.cittofirm.studio.util.stringProperty
import moe.nea.cittofirm.studio.util.useButton
import moe.nea.cittofirm.studio.util.withPrepended
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.column
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.getValue
import tornadofx.hbox
import tornadofx.label
import tornadofx.makeEditable
import tornadofx.scrollpane
import tornadofx.setValue
import tornadofx.tableview
import tornadofx.tooltip
import tornadofx.vbox
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

val sentinelNull = ProjectPath.of(Identifier("cittofirminternal", "models/item/null_model"))
	.intoFile() as GenericModel

val sentinelNullTexture = ProjectPath.of(Identifier("cittofirminternal", "textures/item/null_model.png"))
	.intoFile() as ImageFile

val UIComponent.project get() = workspace as ProjectWindow

class CustomItemModelEditor(
	val model: CustomItemModel,
	val json: JsonObject,
) : UIComponent(model.skyblockItemId) {
	val parentProp = json.stringProperty("parent")
	val headModelProp = json.stringProperty("firmament:head_model")

	inner class Texture private constructor() {
		private var setUp = false
		val nameProperty = object : SimpleStringProperty() {
			override fun invalidated() {
				if (setUp) saveTextureList()
			}
		}
		var name by nameProperty
		val locationProperty = object : SimpleStringProperty() {
			override fun invalidated() {
				if (setUp) saveTextureList()
			}
		}
		var location by locationProperty

		constructor(name: String, location: String) : this() {
			this.name = name
			this.location = location
			this.setUp = true
		}
	}

	fun saveTextureList() {
		val obj = JsonObject()
		textureProp.forEach {
			if (it.location.isNotEmpty())
				obj.addProperty(it.name, it.location)
		}
		json.add("textures", obj)
	}


	val textureProp = run {
		val initial = json["textures"]?.asJsonObject?.entrySet() ?: emptyList()
		val list: ObservableList<Texture> = FXCollections.observableArrayList()
		list.setAll(initial.map { Texture(it.key, it.value.asString) })
		list.addListener(InvalidationListener {
			saveTextureList()
		})
		list
	}


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
				field("Textures") {
					vbox {
						hbox {
							button("Add Texture Reference") {
								action {
									textureProp.add(Texture("layer${textureProp.size}", ""))
								}
							}
							button("Create default Texture") {
								action {
									textureProp.add(Texture("layer0", model.modelIdentifier.toString()))
									val texturePath = model.modelIdentifier.withKnownPath(KnownPath.genericTexture)
									val packFile = ProjectPath.of(texturePath).intoFile()!! as ImageFile
									runAsync {
										val target = packFile.file.resolve(project.resourcePackBase)
										target.createParentDirectories()
										if (!target.exists())
											target.writeBytes(Resources.defaultTexture)
									}
								}
							}
						}
						scrollpane(fitToWidth = true) {
							tableview(textureProp) {
								isEditable = true
								column("Name", Texture::nameProperty).makeEditable().useAutoCompletableTextField {
									FXCollections.observableArrayList(project.modelDataCache.getModelData(Identifier.parse(
										parentProp.value))?.textureNames ?: listOf())
								}
								column("Texture Location", Texture::locationProperty).makeEditable()
									.useAutoCompletableTextField { search ->
										project.textures
											.withPrepended(sentinelNullTexture)
											.observedFilter { Identifier.search(search, it.file.identifier!!) }
											.observedCheapMap {
												if (it == sentinelNullTexture) ""
												else it.textureIdentifier.toString()
											}
									}

								column("Open", { ReadOnlyObjectWrapper(it.value) })
									.useButton("Open") {
										action {
											val file = ProjectPath.of(Identifier.parse(it.value.location)
												                          .withKnownPath(KnownPath.genericTexture))
											project.openFile(file.intoFile()!!)
										}
									}
								column("Delete", { ReadOnlyObjectWrapper(it.value) })
									.useButton("Delete") {
										action {
											textureProp.remove(it.value)
										}
									}
							}
						}
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
				// TODO: Button to open the corresponding file
			}
			button("Save") {
				action {
					model.saveNewJson(project.resourcePackBase, json)
				}
			}
		}
	}
}

