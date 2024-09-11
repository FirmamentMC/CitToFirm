package moe.nea.cittofirm.studio

import javafx.beans.property.ObjectPropertyBase
import javafx.beans.property.Property
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import tornadofx.action
import tornadofx.attachTo
import tornadofx.bind
import tornadofx.getValue
import tornadofx.setValue

class AutoCompletableTextField : TextField() {
	private val contextMenu = ContextMenu()

	val searchFunctionProperty: Property<(String) -> ObservableList<String>> =
		object : ObjectPropertyBase<(String) -> ObservableList<String>>() {
			override fun getBean(): Any {
				return this@AutoCompletableTextField
			}

			override fun getName(): String {
				return "searchFunction"
			}

			override fun invalidated() {
				refreshSearch()
			}
		}
	var searchFunction by searchFunctionProperty

	private fun refreshSearch() {
		if (searchFunctionProperty.value != null)
			contextMenu.items.bind(searchFunction.invoke(text)) { completionOption ->
				MenuItem(completionOption).also {
					it.action {
						this@AutoCompletableTextField.text = completionOption
					}
				}
			}
	}

	init {
		refreshSearch()
		textProperty().addListener { obs, old, new ->
			refreshSearch()
		}
		setOnKeyTyped {
			if (!contextMenu.isShowing) {
				contextMenu.show(this, Side.BOTTOM, 0.0, 0.0)
			}
		}
	}
}

fun EventTarget.autoCompletableTextField(
	searchFunction: ((String) -> ObservableList<String>)? = null,
	op: AutoCompletableTextField.() -> Unit = {}
) {
	AutoCompletableTextField().attachTo(this, op) {
		if (searchFunction != null)
			it.searchFunction = searchFunction
	}
}
