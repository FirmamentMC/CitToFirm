package moe.nea.cittofirm.studio

import javafx.geometry.Pos
import javafx.scene.Parent
import tornadofx.UIComponent
import tornadofx.action
import tornadofx.button
import tornadofx.hbox
import tornadofx.imageview
import tornadofx.vbox
import java.nio.file.Path

class ImageEditor(val imageFile: ImageFile, val filePath: Path) : UIComponent(imageFile.textureIdentifier.toString()) {
	override val root: Parent = vbox(alignment = Pos.CENTER) {
		hbox(alignment = Pos.CENTER) {
			imageview(filePath.toUri().toString()) {
				isSmooth = false
				isScaleShape = true
				fitWidth = 480.0
				fitHeight = 480.0
			}
		}
		button("Open Externally") {
			action {
				project.openExternally(imageFile)
			}
		}
	}

}
