package moe.nea.cittofirm.studio

import io.github.moulberry.repo.data.NEUItem
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension

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

	val extension: String get() = relativePath.extension
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

		private val illegalPathRegex = "[^a-z0-9_.-/]".toRegex()

		fun search(search: String, identifier: Identifier): Boolean {
			if (identifier.namespace == "cittofirminternal") return search.isBlank()
			return identifier.toString().contains(search) // TODO: search segments individually, fuzzy
		}

		fun of(item: NEUItem): Identifier {
			return Identifier("firmskyblock", item.skyblockItemId.lowercase()
				.replace(":", "___")
				.replace(illegalPathRegex) {
					it.value.toCharArray()
						.joinToString("") { "__" + it.code.toString(16).padStart(4, '0') }
				})
		}
	}

	override fun toString(): String {
		return "$namespace:$path"
	}

	fun withKnownPath(knownPath: KnownPath): Identifier = withKnownPath(knownPath.prefix, knownPath.postfix)
	fun withKnownPath(prefix: String, postfix: String): Identifier {
		return Identifier(namespace, prefix + path + postfix)
	}

	fun withoutKnownPath(knownPath: KnownPath): Identifier = withoutKnownPath(knownPath.prefix, knownPath.postfix)
	fun withoutKnownPath(prefix: String, postfix: String): Identifier {
		require(path.startsWith(prefix))
		require(path.endsWith(postfix))
		return Identifier(namespace, path.substring(prefix.length, path.length - postfix.length))
	}
}

data class KnownPath(val prefix: String, val postfix: String) {

	companion object {
		val genericTexture = KnownPath("textures/", ".png")
		val itemModel = KnownPath("models/item/", ".json")
		val genericModel = KnownPath("models/", ".json")
	}
}
