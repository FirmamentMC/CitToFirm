package moe.nea.cittofirm

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.moulberry.repo.IReloadable
import io.github.moulberry.repo.NEURepository
import java.util.*


class SkinCache : IReloadable {
	var skinToNameMap: Map<String, String> = mapOf()

	override fun reload(repository: NEURepository) {
		val map = mutableMapOf<String, String>()
		for (value in repository.items.items.values) {
			val p = LegacyTagParser.parse(value.nbttag)
			val s = p.map["SkullOwner"] as? NbtCompound ?: continue
			val pr = s.map["Properties"] as? NbtCompound ?: continue
			val t = pr.map["textures"] as? NbtList ?: continue
			val z = t.list[0] as NbtCompound
			val v = z.map["Value"] as? NbtString ?: continue
			map[encodedToSkinUrl(v.string)] = value.skyblockItemId
		}
		skinToNameMap = map
	}

	private val g = Gson()
	fun encodedToSkinUrl(encoded: String): String {
		var encodedF: String = encoded
		while (encodedF.length % 4 != 0 && encodedF.last() == '=') {
			encodedF = encodedF.substring(0, encodedF.length - 1)
		}
		runCatching {
			val rawJson = Base64.getDecoder()
				.decode(encodedF)
				.decodeToString()
			val json = g.fromJson(rawJson, JsonObject::class.java)
			return json.get("textures").asJsonObject.get("SKIN").asJsonObject.get("url").asString
		}.getOrElse {
			it.printStackTrace()
			return ""
		}
	}

}