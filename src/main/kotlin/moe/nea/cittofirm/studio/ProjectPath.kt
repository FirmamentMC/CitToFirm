package moe.nea.cittofirm.studio

import java.nio.file.Path
import kotlin.io.path.Path

data class ProjectPath(
	val relativePath: Path,
) : Comparable<ProjectPath> {
	companion object {
		fun of(root: Path, actual: Path) = ProjectPath(root.relativize(actual))
		fun of(identifier: Identifier) = ProjectPath(Path("assets/${identifier.namespace}/${identifier.path}"))
		private val identifierPattern =
			"assets/([^/]+)/(.*)".toRegex()
		private val customItemModelPathPattern =
			"models/item/([a-z0-9_.\\-]+)\\.json".toRegex()
	}

	fun resolve(basePath: Path): Path {
		return basePath.resolve(relativePath)
	}

	fun isModel(prefix: String): Boolean = identifier?.path?.startsWith("models/$prefix") == true

	val identifier = identifierPattern.matchEntire(relativePath.toString())?.let {
		Identifier(it.groupValues[1], it.groupValues[2])
	}

	fun intoFile(): ResourcePackFile? {
		if (identifier?.path?.endsWith(".png") == true)
			return ImageFile(this)
		getCustomItemModel()?.let { return it }
		getGenericModel()?.let { return it }
		return null
	}

	fun getGenericModel(): NonCustomItemModel? {
		if (isModel("")) return NonCustomItemModel(this)
		return null
	}


	fun getCustomItemModel(): CustomItemModel? {
		if (identifier?.namespace != "firmskyblock") return null
		val path = identifier.path.let(customItemModelPathPattern::matchEntire)
			?.let { it.groupValues[1] }
			?: return null
		return CustomItemModel(path, this)
	}

	override fun compareTo(other: ProjectPath): Int {
		return String.CASE_INSENSITIVE_ORDER.compare(this.relativePath.toString(), other.relativePath.toString())
	}
}

data class Identifier(
	val namespace: String,
	val path: String,
) {
	companion object {
		fun parse(string: String): Identifier {
			val split = string.split(":", limit = 2)
			if (split.size == 1) {
				return Identifier("minecraft", split[0])
			}
			return Identifier(split[0], split[1])
		}

		fun search(search: String, identifier: Identifier): Boolean {
			return identifier.toString().contains(search) // TODO: search segments individually, fuzzy
		}
	}

	override fun toString(): String {
		return "$namespace:$path"
	}
}