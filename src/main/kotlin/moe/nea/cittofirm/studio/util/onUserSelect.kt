package moe.nea.cittofirm.studio.util

import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TreeTableRow
import javafx.scene.control.TreeTableView
import javafx.scene.control.skin.TableColumnHeader
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import tornadofx.selectedItem

fun EventTarget.isInsideRowNea(): Boolean {
	if (this !is Node)
		return false

	if (this is TableColumnHeader)
		return false

	if (this is TableRow<*> || this is TableView<*> || this is TreeTableRow<*> || this is TreeTableView<*> || this is ListCell<*>)
		return true

	if (this.parent != null)
		return this.parent.isInsideRowNea()

	return false
}

fun <T> ListView<T>.onUserSelectNea(clickCount: Int = 2, action: (T) -> Unit) {
	addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
		val selectedItem = this.selectedItem
		if (event.clickCount == clickCount && selectedItem != null && event.target.isInsideRowNea())
			action(selectedItem)
	}

	addEventFilter(KeyEvent.KEY_PRESSED) { event ->
		val selectedItem = this.selectedItem
		if (event.code == KeyCode.ENTER && !event.isMetaDown && selectedItem != null)
			action(selectedItem)
	}
}
