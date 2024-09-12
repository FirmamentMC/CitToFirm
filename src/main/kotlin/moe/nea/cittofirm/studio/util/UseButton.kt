package moe.nea.cittofirm.studio.util

import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn

fun <S, P, T : TableColumn<P, S>> T.useButton(text: String, function: Button.(S) -> Unit): T {
	cellFactory = Callback {
		object : TableCell<P, S>() {
			val button = Button().also {
				it.text = text
				function(it, this.item)
			}

			override fun updateItem(p0: S?, p1: Boolean) {
				super.updateItem(p0, p1)
				if (isEmpty) {
					graphic = null
				} else {
					graphic = button
				}
			}
		}
	}
	return this
}