package moe.nea.cittofirm.studio

import tornadofx.App
import tornadofx.UIComponent
import tornadofx.launch
import kotlin.io.path.Path


fun main(args: Array<String>) {
	launch<FirmStudio>(args)
}

class FirmStudio : App(MainWindow::class) {
	override fun onBeforeShow(view: UIComponent) {
	}
}