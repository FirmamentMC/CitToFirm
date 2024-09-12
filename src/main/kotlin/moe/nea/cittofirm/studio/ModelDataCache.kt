package moe.nea.cittofirm.studio

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlin.io.path.exists
import kotlin.io.path.readText

class ModelDataCache(val project: ProjectWindow) {

	data class ModelData(
		val textureNames: List<String>,
	) {
		constructor(vararg names: String) : this(names.toList())
	}

	fun invalidateCachePath(path: ProjectPath) {
		val model = path.intoFile() as? GenericModel
		map.remove(model?.modelIdentifier)
	}

	private val map = mutableMapOf<Identifier, ModelData>()

	init {
		map[Identifier("minecraft", "item/generated")] = ModelData("layer0")
		map[Identifier("minecraft", "item/handheld")] = ModelData("layer0")
	}

	// TODO: make this function async somehow (or maybe index in the background)
	private fun load(modelId: Identifier): ModelData {
		val model = ProjectPath.of(modelId.withKnownPath(KnownPath.genericModel))
			.intoFile() as GenericModel
		val path = project.getRealPath(model)
		if (!path.exists()) {
			return ModelData(listOf()) // TODO empty fallback
		}
		val json = Gson().fromJson(path.readText(), JsonObject::class.java)
		val textureNames = json["parent"]?.asString?.let { parent ->
			getModelData(Identifier.parse(parent))
		}?.textureNames?.toMutableSet() ?: mutableSetOf()
		json["textures"]?.asJsonObject?.let { textures ->
			for (mutableEntry in textures.entrySet()) {
				val value = mutableEntry.value.asString
				if (value.startsWith("#"))
					textureNames.add(value.substring(1))
			}
		}
		json["elements"]?.asJsonArray?.let { elements ->
			for (element in elements) {
				element.asJsonObject["faces"]?.asJsonObject?.let {
					for (face in it.entrySet()) {
						val textureName = face.value.asJsonObject["texture"]?.asString
						if (textureName?.startsWith("#") == true) {
							textureNames.add(textureName.substring(1))
						}
					}
				}
			}
		}
		return ModelData(textureNames.toList())
	}


	fun getModelData(modelId: Identifier): ModelData? {
		return map.computeIfAbsent(modelId, ::load)
	}

}
