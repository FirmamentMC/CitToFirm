package moe.nea.cittofirm.studio

import javafx.beans.property.ObjectPropertyBase
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventTarget
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.util.Callback
import tornadofx.action
import tornadofx.attachTo
import tornadofx.bind
import tornadofx.getValue
import tornadofx.setValue


fun <S, T : TableColumn<S, String>> T.useAutoCompletableTextField(searchFunction: ((String) -> ObservableList<String>)): T {
	this.cellFactory = Callback { AutoCompletableTextFieldTableCell.withSearch(searchFunction) }
	return this
}

class AutoCompletableTextFieldTableCell<S> : TableCell<S, String>() {
	init {
		styleClass.add("text-field-table-cell")
	}

	companion object {
		fun <S> withSearch(searchFunction: ((String) -> ObservableList<String>)): AutoCompletableTextFieldTableCell<S> {
			return AutoCompletableTextFieldTableCell<S>().also { it.searchFunction = searchFunction }
		}
	}

	private var searchFunction: ((String) -> ObservableList<String>)? = null
	val textField by lazy {
		val field = AutoCompletableTextField()
		field.onEscape = {
			cancelEdit()
			true
		}
		field.searchFunction = searchFunction
		field.setOnAction {
			commitEdit(field.text)
			it.consume()
		}
		field
	}

	override fun startEdit() {
		super.startEdit()
		if (this.isEditing) {
			textField.text = item
			this.text = null
			this.graphic = textField
			textField.selectAll()
			textField.requestFocus()
		}
	}

	override fun cancelEdit() {
		super.cancelEdit()
		this.text = item
		this.graphic = null
	}

	override fun updateItem(p0: String?, p1: Boolean) {
		super.updateItem(p0, p1)
		if (isEmpty) {
			this.text = null
			this.graphic = null
		} else if (this.isEditing) {
			this.textField.text = item
			this.text = null
			this.graphic = this.textField
		} else {
			this.text = item
			this.graphic = null
		}
	}
}

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
						onAction.handle(ActionEvent())
					}
				}
			}
	}

	val onEscapeProperty = SimpleObjectProperty<() -> Boolean>()
	var onEscape by onEscapeProperty

	init {
		refreshSearch()
		textProperty().addListener { obs, old, new ->
			refreshSearch()
		}
		setOnKeyTyped {
			// TODO: insert text and select it (so that the next keystroke replaces it)
			if (!contextMenu.isShowing) {
				contextMenu.show(this, Side.BOTTOM, 0.0, 0.0)
			}
		}
		setOnKeyReleased {
			if (it.code == KeyCode.ESCAPE) {
				onEscape?.invoke()
				it.consume()
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
