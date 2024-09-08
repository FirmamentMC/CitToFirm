package moe.nea.cittofirm.studio

import tornadofx.App
import tornadofx.launch


fun main(args: Array<String>) {
	launch<FirmStudio>(args)
}

class FirmStudio : App(MainWindow::class)