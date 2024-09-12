package moe.nea.cittofirm.studio

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import tornadofx.CssBox
import tornadofx.MultiValue
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.hbox
import tornadofx.label
import tornadofx.px
import tornadofx.style
import tornadofx.text
import tornadofx.vbox
import java.awt.Desktop

class ErrorEditor(name: String, val file: ResourcePackFile) : UIComponent("Error - $name") {
	override val root = vbox(alignment = Pos.CENTER) {
		hbox {
			alignment = Pos.CENTER
			vbox(alignment = Pos.CENTER) {
				padding = Insets(3.0)
				style {
					backgroundColor = MultiValue(arrayOf(Color.RED.interpolate(Color.TRANSPARENT, 0.4)))
					backgroundRadius = MultiValue(arrayOf(CssBox(15.px, 15.px, 15.px, 15.px)))
				}
				label("Error!") {
					textAlignment = TextAlignment.CENTER
					style {
						fontSize = 30.px
					}
				}
				text("Could not edit file $name. Either loading this file caused an error, or this file cannot be edited in FirmStudio.") {
					wrappingWidthProperty().set(300.0)
				}
				button("Open externally") {
					action {
						project.openExternally(file)
					}
				}
				autosize()
			}
		}
	}
}