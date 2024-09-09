package moe.nea.cittofirm.studio

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object Settings {
	private val gson = GsonBuilder()
		.setPrettyPrinting()
		.create()

	private fun save() {
		configPath.createParentDirectories()
		configPath.writeText(gson.toJson(jsonObject))
	}

	private val configPath = Path(System.getProperty("user.home")) / ".config/FirmStudio/config.json"
	private val jsonObject = (
			if (configPath.exists()) runCatching {
				gson.fromJson(configPath.readText(), JsonObject::class.java)
			}.getOrNull() else null) ?: JsonObject()

	private inline fun <T, P : Property<T>> setting(
		name: String,
		propertyConstructor: () -> P,
		default: () -> T,
		crossinline fromJson: (JsonElement) -> T,
		crossinline toJson: (T) -> JsonElement,
	): P {
		val prop = propertyConstructor()
		prop.value = jsonObject[name]?.let(fromJson) ?: default()
		prop.addListener { v, o, n ->
			jsonObject.add(name, toJson(n))
			save()
		}
		return prop
	}

	private fun boolSetting(name: String, default: () -> Boolean) =
		setting(
			name,
			{ SimpleBooleanProperty() },
			default,
			{ it.asBoolean },
			{ JsonPrimitive(it) })

	val enableFileWatcher = boolSetting("enableFileWatcher") { true }
	val darkMode = boolSetting("darkMode") { false }
}