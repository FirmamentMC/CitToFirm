package moe.nea.cittofirm.studio

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.collections.transformation.FilteredList
import moe.nea.cittofirm.studio.util.onUserSelectNea
import tornadofx.Workspace
import tornadofx.action
import tornadofx.checkmenuitem
import tornadofx.fitToParentSize
import tornadofx.hbox
import tornadofx.information
import tornadofx.item
import tornadofx.listview
import tornadofx.menu
import tornadofx.menubar
import tornadofx.scrollpane
import tornadofx.text
import tornadofx.textarea
import tornadofx.textfield
import tornadofx.vbox
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.function.Predicate
import kotlin.io.path.name
import kotlin.streams.asSequence

class ProjectWindow(
	val resourcePackBase: Path,
) : Workspace("FirmStudio - " + resourcePackBase.name) {
	lateinit var debugStream: PrintStream
	init {
		require(resourcePackBase.isAbsolute)
	}
	data class ProgressElement(
		val label: String
	)

	val files: ObservableMap<
			ProjectPath,
			ResourcePackFile> = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap())
	val fileList = run {
		val list = FXCollections.observableArrayList<ResourcePackFile>()
		files.addListener(MapChangeListener { change ->
			if (change.valueAdded != null)
				list.add(change.valueAdded)
			if (change.valueRemoved != null)
				list.removeAll(change.valueRemoved)
			FXCollections.sort(list)
		})
		FXCollections.unmodifiableObservableList(list)
	}
	val watchService = RecursiveWatchService(resourcePackBase, Duration.ofMillis(400L)) {
		Platform.runLater {
			it.forEach { updateFile(it.file) }
		}
	}

	fun updateFile(path: Path) {
		val actualPath = resourcePackBase.resolve(path)
		val pp = ProjectPath.of(resourcePackBase, actualPath)
		val file = if (Files.isRegularFile(actualPath)) pp.intoFile() else null
		if (file != null) {
			files[pp] = pp.intoFile()
		} else {
			files.remove(pp)
		}
	}

	fun scanDirectory() {
		files.clear()
		Files.walk(resourcePackBase).asSequence().forEach(::updateFile)
	}

	override fun onUndock() {
		watchService.close()
	}

	init {
		menubar {
			menu("File") {
				item("Exit").action {
					Platform.exit()
				}
			}
			menu("Import") {
				item("Import from CIT") {
					// TODO: show import dialogue
				}
			}
			menu("Settings") {
				checkmenuitem("Enable File Watcher") {
					selectedProperty().bindBidirectional(Settings.enableFileWatcher)
				}
				checkmenuitem("Dark Mode") {
					selectedProperty().bindBidirectional(Settings.darkMode)
				}
			}
			menu("Help") {
				item("Discord").action {
					hostServices.showDocument("https://discord.gg/64pFP94AWA")
				}
				item("About...").action {
					information("About FirmStudio",
					            "CitToFirm v0.0.0\nAuthored by Linnea Gräf")
				}
			}
		}
		with(leftDrawer) {
			fixedContentSize = 350.0
			item("Files") {
				vbox {
					fitToParentSize()
					val filtered = FilteredList(fileList)
					textfield {
						promptText = "Search"
						textProperty().addListener { obs, old, new ->
							filtered.setPredicate(createFileFilter(new))
						}
					}
					scrollpane(fitToWidth = true, fitToHeight = true) {
						fitToParentSize()
						listview(filtered) {
							cellFormat {
								graphic = hbox {
									text(it.file.identifier?.toString() ?: "<error>")
								}
							}
							onUserSelectNea {
								runAsync {
									it.openUI(resourcePackBase)
								} ui {
									dock(it)
								}
							}
						}
					}
				}
			}
			item("Items") {
			}
		}
		with(bottomDrawer) {
			item("Debug Logs") {
				textarea {
					isEditable = false
					val os = object : OutputStream() {
						override fun write(b: Int) {
							TODO("Not yet implemented")
						}

						override fun write(b: ByteArray, off: Int, len: Int) {
							this@textarea.appendText(String(b.copyOfRange(off, len), StandardCharsets.UTF_8))
						}
					}
					debugStream = PrintStream(os, false, StandardCharsets.UTF_8.name())
				}
			}
		}
		scanDirectory()
	}

	private fun createFileFilter(new: String): Predicate<in ResourcePackFile> {
		val words = new.split(" ").filter { it.isNotBlank() }
		return Predicate {
			val searchText = it.file.identifier.toString()
			words.all {
				searchText.contains(it)
			}
		}
	}
}
