package moe.nea.cittofirm.studio

import io.github.moulberry.repo.data.NEUItem
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.collections.transformation.FilteredList
import javafx.scene.control.ButtonType
import javafx.stage.FileChooser
import moe.nea.cittofirm.CitNormalizer
import moe.nea.cittofirm.CitTransformer
import moe.nea.cittofirm.studio.model.McMeta
import moe.nea.cittofirm.studio.util.onUserSelectNea
import tornadofx.Workspace
import tornadofx.action
import tornadofx.checkmenuitem
import tornadofx.chooseFile
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
import tornadofx.warning
import java.awt.Desktop
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate
import javax.swing.SwingUtilities
import kotlin.concurrent.withLock
import kotlin.io.path.createParentDirectories
import kotlin.io.path.name
import kotlin.io.path.writeText

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
		runAsync {
			Files.walk(resourcePackBase).use { it.toList() }
		} ui {
			it.forEach(::updateFile)
		}
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
				item("Reload Files from Disk").action {
					scanDirectory()
				}
				item("Exit").action {
					Platform.exit()
				}
			}
			menu("Import") {
				item("Import from CIT").action {
					val file = chooseFile("Select an Optifine CIT pack",
					                      arrayOf(FileChooser.ExtensionFilter("Minecraft Resource Pack", "*.zip")))
						.singleOrNull() ?: return@action
					runAsync {
						McMeta.getMeta(file)
					} ui { mcMeta ->
						if (mcMeta == null &&
							warning("Invalid resourcepack",
							        "The specified file is either not a zip or missing a pack.mcmeta file. Are you sure you want to continue?",
							        ButtonType.CANCEL, ButtonType.YES).result != ButtonType.YES
						) {
							return@ui
						}
						if (mcMeta?.pack?.packFormat != 1 &&
							warning("Invalid resourcepack version",
							        "The specified resourcepack is for pack format ${mcMeta?.pack?.packFormat}, expected pack format 1. Are you sure you want to continue?",
							        ButtonType.CANCEL, ButtonType.YES).result != ButtonType.YES
						) {
							return@ui
						}
						if (warning("Warning",
						            "Importing an old CIT file can override existing files. Make sure you have a proper back up of any important files in this pack. Are you sure you want to continue?",
						            ButtonType.CANCEL, ButtonType.YES).result != ButtonType.YES
						) {
							return@ui
						}
						importPack(file)
					}
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
					val buffer = StringBuffer()
					val lock = ReentrantLock()
					var hasScheduledUpdate = false
					fun createForkStream(fd: FileDescriptor): PrintStream {
						val oldOs = FileOutputStream(fd)
						val os = object : OutputStream() {
							override fun write(b: Int) {
								TODO("Not yet implemented")
							}

							override fun write(b: ByteArray, off: Int, len: Int) {
								val recons = String(b.copyOfRange(off, len), StandardCharsets.UTF_8)
								lock.withLock {
									buffer.append(recons)
									if (!hasScheduledUpdate) {
										hasScheduledUpdate = true
										Platform.runLater {
											lock.withLock {
												hasScheduledUpdate = false
												this@textarea.appendText(buffer.toString())
												buffer.setLength(0)
											}
										}
									}
								}
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

	fun importPack(packFile: File) {
		tornadofx.runAsync {
			val normalized = XDGPaths.tempDir("normalized")
			val source = FileSystems.newFileSystem(packFile.toPath()).getPath("/")
			println("Normalizing paths and CIT properties from $packFile to $normalized")
			CitNormalizer.normalize(source, normalized)
			println("Pack CIT normalization complete")
			println("Creating CIT transformer")
			val trans = CitTransformer(normalized, resourcePackBase, RepoService.repository, RepoService.skinCache)
			trans.setup(false)
			println("Created CIT transformer")
			println("Discovering CIT directives")
			trans.discover()
			println("Discovered ${trans.directives.size} directives")
			println("Generating model files")
			trans.generate()
			println("Generated model files")
			println("CIT import completed")
		} ui {
			information("CIT import complete", "CIT import completed. Make sure to check all generated files.")
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
