package moe.nea.cittofirm

import io.github.moulberry.repo.NEURepository
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class CitTransformer(val source: Path, val target: Path, val repo: NEURepository, val skinCache: SkinCache) {
    fun setup() {
        require(source.isDirectory())
        require(source.resolve("pack.mcmeta").isRegularFile())
        target.createDirectories()
        target.forEach { it.deleteRecursively() }
    }

    val citRoot = source.resolve("assets/minecraft/mcpatcher/cit/")
    lateinit var directives: List<Directive>

    fun discover() {
        directives = citRoot.walk().filter { it.isRegularFile() }
            .filter { it.extension == "properties" }
            .mapNotNull { parsePropertyFile(it) }
            .toList()
    }

    fun generate() {
        generateMCMeta()
        directives.forEach { generateDirective(it) }
    }

    fun targetFile(path: String): Path {
        val p = target.resolve(path)
        p.createParentDirectories()
        return p
    }

    fun generateDirective(directive: Directive) {
        val modelName: String
        if (directive.modelData.modelPath != null) {
            val smn = directive.modelData.modelPath.relativeTo(source.resolve("assets/minecraft")).toString()
                .replace("/", "_").replace(".json", "")
            modelName = "cittofirmgenerated:item/$smn"
            val t = targetFile("assets/cittofirmgenerated/models/item/$smn.json")
            if (!t.exists())
                directive.modelData.modelPath.copyTo(t)
        } else {
            modelName = "minecraft:item/handheld"
        }
        var textureName: String? = null
        if (directive.modelData.texturePath != null) {
            val id = directive.modelData.texturePath.relativeTo(source.resolve("assets/minecraft")).toString()
                .replace("/", "_").replace(".png", "")
            textureName = "cittofirmgenerated:item/$id"
            val t = targetFile("assets/cittofirmgenerated/textures/item/$id.png")
            if (!t.exists()) {
                directive.modelData.texturePath.copyTo(t)
                val n = directive.modelData.texturePath.resolveSibling(directive.modelData.texturePath.name + ".mcmeta")
                if (n.isRegularFile()) {
                    n.copyTo(targetFile("assets/cittofirmgenerated/textures/item/$id.png.mcmeta"))
                }
            }
        }
        directive.skyblockIds.forEach {
            val replacedId = it.replace(";", "__").replace(":", "___").lowercase()
            var t = "{"
            if (textureName != null)
                t += """
                "textures": {
                    "layer0": "$textureName"
                },
            """.trimIndent()
            targetFile("assets/firmskyblock/models/item/$replacedId.json")
                .writeText(
                    """$t"parent": "$modelName"}"""
                )
        }
    }

    fun generateMCMeta() {
        val packFormat = """
            {
              "pack": {
                "pack_format": 15,
                "description": "Generated via CITToFirm"
              }
            }
        """.trimIndent()
        targetFile("pack.mcmeta").writeText(packFormat)
    }

    fun parsePropertyFile(path: Path): Directive? {
        val properties = Properties().also { it.load(path.inputStream()) }
        if (properties["type"] == "armor") return null
        val skyblockId = findSkyblockId(properties)
        if (skyblockId.isEmpty()) {
            println("Could not find skyblock id for $path")
            return null
        }
        if (skyblockId.any { repo.items.getItemBySkyblockId(it) == null }) {
            println("Skyblock id $skyblockId could not resolve to an item at $path")
            return null
        }
        val modelData = findTexture(path, properties)
        if (modelData.modelPath == null && modelData.texturePath == null) {
            println("Could not find model data for $path")
            return null
        }
        return Directive(skyblockId, modelData)
    }

    data class Directive(
        val skyblockIds: List<String>,
        val modelData: ModelData,
    )

    data class ModelData(
        val modelPath: Path?,
        val texturePath: Path?,
    )

    fun findTexture(path: Path, properties: Properties): ModelData {
        val model = properties["model"]
        val modelPath = if (model is String) {
            resolveIdentifier(model, "models", ".json") ?: path.resolveSibling("$model.json")
        } else {
            require(model == null)
            null
        }
        val texture = properties["texture"]
        val texturePath = if (texture is String) {
            resolveIdentifier(texture, extension = ".png") ?: path.resolveSibling("$texture.png")
        } else {
            require(texture == null)
            path.resolveSibling(path.nameWithoutExtension + ".png").takeIf { it.isRegularFile() }
        }
        return ModelData(modelPath, texturePath)
    }

    fun resolveIdentifier(ident: String, prefix: String? = null, extension: String = ""): Path? {
        val s = ident.split(":")
        require(s.size == 1 || s.size == 2)
        val namespace = if (s.size == 1) "minecraft" else s[0]
        val path = s.last()
        val p = if (prefix == null) "" else "/$prefix"
        return source.resolve("assets/$namespace$p/$path$extension").takeIf { it.isRegularFile() }
    }

    fun findSkyblockId(properties: Properties): List<String> {
        val fromExtraAttributes = properties["nbt.ExtraAttributes.id"]
        if (fromExtraAttributes is String) {
            if (fromExtraAttributes.startsWith("regex:")) {
                val pattern = fromExtraAttributes.substring("regex:".length).toPattern().asMatchPredicate()
                return repo.items.items!!.keys.asSequence().filter { pattern.test(it) }.toList()
            }
            return listOf(fromExtraAttributes)
        }
        val fromSkin = properties["nbt.SkullOwner.Properties.textures.0.Value"]
        if (fromSkin is String) {
            val s = skinCache.encodedToSkinUrl(fromSkin)
            val k = skinCache.skinToNameMap[s]
            if (k != null)
                return listOf(k)
        }
        return listOf()
    }


}