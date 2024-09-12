package moe.nea.cittofirm.studio

import io.github.moulberry.repo.data.NEUItem
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
import java.awt.Desktop
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.function.Predicate
import javax.swing.SwingUtilities
import kotlin.io.path.createParentDirectories
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.streams.asSequence

class ProjectWindow(
	val resourcePackBase: Path,
) : Workspace("FirmStudio - " + resourcePackBase.name) {
	init {
		scope.workspace = this
	}

	init {
		require(resourcePackBase.isAbsolute)
	}

	val files: ObservableMap<
			ProjectPath,
			ResourcePackFile> = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap())
	val fileList = run {
		val list = FXCollections.observableArrayList<ResourcePackFile>()
		files.addListener(MapChangeListener { change ->
			assert(Platform.isFxApplicationThread())
			if (change.valueAdded != null)
				list.add(change.valueAdded)
			if (change.valueRemoved != null)
				list.removeAll(change.valueRemoved)
			FXCollections.sort(list)
		})
		FXCollections.unmodifiableObservableList(list)
	}
	val modelDataCache = ModelDataCache(this)

	val models: FilteredList<GenericModel> = run {
		FilteredList(fileList) {
			it is GenericModel
		} as FilteredList<GenericModel>
	}
	val textures: FilteredList<ImageFile> = run {
		FilteredList(fileList) {
			it is ImageFile
		} as FilteredList<ImageFile>
	}

	val watchService = RecursiveWatchService(resourcePackBase, Duration.ofMillis(400L)) {
		Platform.runLater {
			it.forEach { updateFile(it) }
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

	fun openFile(it: ResourcePackFile) {
		assert(Platform.isFxApplicationThread())
		val gui = kotlin.runCatching {
			it.openUI(resourcePackBase)
		}.getOrElse { ex ->
			ex.printStackTrace()
			ErrorEditor(it.file.identifier.toString(), it)
		}
		dock(gui)
	}

	init {
		menubar {
			menu("File") {
				item("Close Project").action {
					replaceWith(MainWindow())
				}
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
								openFile(it)
							}
						}
					}
				}
			}
			item("Items") {
				vbox {
					fitToParentSize()
					val filtered = FilteredList(RepoService.items)
					textfield {
						promptText = "Search"
						textProperty().addListener { obs, old, new ->
							filtered.setPredicate(createItemFilter(new))
						}
					}
					scrollpane(fitToWidth = true, fitToHeight = true) {
						fitToParentSize()
						listview(filtered) {
							cellFormat {
								graphic = hbox {
									text(RepoService.getDisplayName(it.skyblockItemId))
								}
							}
							onUserSelectNea {
								runAsync {
									val path = ProjectPath.of(Identifier.of(it).withKnownPath(KnownPath.itemModel))
									val file = path.intoFile()!!
									val filePath = path.resolve(resourcePackBase)
									if (!Files.exists(filePath)) {
										filePath.createParentDirectories()
										filePath.writeText(Resources.defaultModel) // TODO: replace texture maybe?
									}
									file
								} ui {
									openFile(it)
								}
							}
						}
					}
				}
			}
		}
		with(bottomDrawer) {
			item("Debug Logs") {
				textarea {
					isEditable = false
					fun createForkStream(fd: FileDescriptor): PrintStream {
						val oldOs = FileOutputStream(fd)
						val os = object : OutputStream() {
							override fun write(b: Int) {
								TODO("Not yet implemented")
							}

							override fun write(b: ByteArray, off: Int, len: Int) {
								val recons = String(b.copyOfRange(off, len), StandardCharsets.UTF_8)
								this@textarea.appendText(recons)
								oldOs.write(b, off, len)
							}
						}
						return PrintStream(os, false, StandardCharsets.UTF_8.name())
					}
					System.setErr(createForkStream(FileDescriptor.err))
					System.setOut(createForkStream(FileDescriptor.out))
				}
			}
		}
		scanDirectory()
	}

	private fun createItemFilter(new: String): Predicate<in NEUItem> {
		val words = new.split(" ").filter { it.isNotBlank() }
		return Predicate {
			val searchText = it.skyblockItemId + it.displayName + it.lore.joinToString()
			words.all { searchText.contains(it, ignoreCase = true) }
		}
	}

	private fun createFileFilter(new: String): Predicate<in ResourcePackFile> {
		val words = new.split(" ").filter { it.isNotBlank() }
		return Predicate {
			val searchText = it.file.identifier.toString()
			words.all {
				searchText.contains(it, ignoreCase = true)
			}
		}
	}

	fun openExternally(packFile: ResourcePackFile) {
		val editor = ExternalEditor.editors.find { it.canOpen(packFile) }
		if (editor != null) {
			editor.open(this, packFile)
		} else {
			SwingUtilities.invokeLater {
				Desktop.getDesktop().browseFileDirectory(getRealPath(packFile).toFile())
			}
		}
	}

	fun getRealPath(packFile: ResourcePackFile): Path {
		return packFile.file.resolve(resourcePackBase)
	}
}
