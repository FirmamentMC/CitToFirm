package moe.nea.cittofirm.studio.util

import com.google.gson.JsonObject
import javafx.beans.property.ObjectPropertyBase

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