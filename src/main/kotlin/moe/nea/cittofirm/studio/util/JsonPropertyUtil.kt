package moe.nea.cittofirm.studio.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javafx.beans.property.ObjectPropertyBase
import javafx.beans.property.Property
import javafx.collections.ObservableListBase
import tornadofx.getValue

fun JsonObject.objectProperty(name: String): ObjectPropertyBase<JsonObject> {
	val prop = object : ObjectPropertyBase<JsonObject>() {
		override fun getBean(): Any {
			return this@objectProperty
		}

		override fun invalidated() {
			super.invalidated()
			val value = get()
			this@objectProperty.add(name, if (value.isEmpty) null else value)
		}

		override fun getName(): String {
			return name
		}
	}
	prop.set(this[name]?.asJsonObject ?: JsonObject())
	return prop
}

fun JsonObject.stringProperty(name: String): ObjectPropertyBase<String> {
	val prop = object : ObjectPropertyBase<String>() {
		override fun getBean(): Any {
			return this@stringProperty
		}

		override fun invalidated() {
			super.invalidated()
			val value = get()
			this@stringProperty.addProperty(name, value.ifEmpty { null })
		}

		override fun getName(): String {
			return name
		}
	}
	prop.set(this[name]?.asString ?: "")
	return prop
}