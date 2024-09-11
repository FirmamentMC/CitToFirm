package moe.nea.cittofirm.studio

import javafx.stage.Stage
import tornadofx.App
import tornadofx.UIComponent
import tornadofx.launch


fun main(args: Array<String>) {
	launch<FirmStudio>(args)
}

class FirmStudio : App(MainWindow::class) {
	override fun onBeforeShow(view: UIComponent) {
	}


	override fun start(stage: Stage) {
		super.start(stage)
		val darkModeUri = FirmStudio::class.java.getResource("/dark-theme.css")!!.toURI()
		fun updateDarkModeTo(new: Boolean) {
			if (new)
				stage.scene.stylesheets.add(darkModeUri.toString())
			else
				stage.scene.stylesheets.remove(darkModeUri.toString())
		}
		stage.sceneProperty().addListener { obv, old, new ->
			updateDarkModeTo(Settings.darkMode.value)
		}
		updateDarkModeTo(Settings.darkMode.value)
		Settings.darkMode.addListener { obv, old, new ->
			updateDarkModeTo(new)
		}
	}
}