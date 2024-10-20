package moe.nea.cittofirm.studio

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
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
import tornadofx.singleAssign


fun <S, T : TableColumn<S, String>> T.useAutoCompletableTextField(searchResults: ((ObservableValue<String>) -> ObservableList<String>)): T {
	this.cellFactory = Callback { AutoCompletableTextFieldTableCell.withSearch(searchResults) }
	return this
}

class AutoCompletableTextFieldTableCell<S> : TableCell<S, String>() {
	init {
		styleClass.add("text-field-table-cell")
	}

	companion object {
		fun <S> withSearch(searchResults: ((ObservableValue<String>) -> ObservableList<String>)?): AutoCompletableTextFieldTableCell<S> {
			return AutoCompletableTextFieldTableCell<S>().also { it.searchResults = searchResults }
		}
	}

	private var searchResults: ((ObservableValue<String>) -> ObservableList<String>)? = null
	val textField by lazy {
		val field = AutoCompletableTextField()
		field.onEscape = {
			cancelEdit()
			true
		}
		field.searchResults = searchResults!!.invoke(this.itemProperty())
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

	private var _searchResults by singleAssign<ObservableList<String>>()
	var searchResults
		set(value) {
			// These values are single assign due to the bugged nature of .bind not unbinding old listeners.
			// TODO: MutableList<TargetType>.bind does not allow for changing bindings
			// Is this worth raising an issue?
			// You can unbind old lists since the listener is returned. But this means keeping track of the last list
			// It would be nice to just set a flag in the listener instead.
			contextMenu.items.bind(value) { completionOption ->
				MenuItem(completionOption).also {
					it.action {
						this@AutoCompletableTextField.text = completionOption
						onAction?.handle(ActionEvent())
					}
				}
			}
			_searchResults = value
		}
		get() = _searchResults


	val onEscapeProperty = SimpleObjectProperty<() -> Boolean>()
	var onEscape: (() -> Boolean)? by onEscapeProperty

	init {
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
	searchResults: ObservableList<String>? = null,
	op: AutoCompletableTextField.() -> Unit = {}
) {
	AutoCompletableTextField().attachTo(this, op) {
		if (searchResults != null)
			it.searchResults = searchResults
	}
}
