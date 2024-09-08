package moe.nea.cittofirm.studio

import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import tornadofx.View
import tornadofx.ViewTransition
import tornadofx.button
import tornadofx.chooseDirectory
import tornadofx.hbox
import tornadofx.label
import tornadofx.px
import tornadofx.seconds
import tornadofx.style
import tornadofx.vbox
import tornadofx.warning
import kotlin.jvm.optionals.getOrNull

class MainWindow : View() {
	override val root = vbox(alignment = Pos.CENTER) {
		label("Welcome to FirmStudio") {
			style {
				fontSize = 40.px
			}
		}
		hbox(alignment = Pos.CENTER) {
			button("Open Project") {
				this.setOnAction {
					val directory = chooseDirectory("Open Project Folder") ?: return@setOnAction
					val shouldOpen = directory.resolve("pack.mcmeta").exists() ||
							warning("Not a resource pack",
							        "The folder you selected does not contain a pack.mcmeta file which is required for a resourcepack to work. Open anyway?",
							        ButtonType.YES, ButtonType.NO).showAndWait().getOrNull() == ButtonType.YES
					if (shouldOpen){
						replaceWith(
							transition = ViewTransition.Slide(0.2.seconds), sizeToScene = true,
							replacement = ProjectWindow(directory.toPath()))
					}
				}
			}
			button("New Project")
		}
	}

}
