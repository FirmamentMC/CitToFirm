package moe.nea.cittofirm.studio

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

	fun getModelData(modelId: Identifier): ModelData? {
		// TODO: actually read out file contents here
		return map[modelId]
	}

}
