package moe.nea.cittofirm.studio

import io.github.moulberry.repo.NEURepository
import io.github.moulberry.repo.data.NEUItem
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText

object RepoService {
	private val baseDir = XDGPaths.cache() / "FirmStudio/repository"
	private val etagFile = baseDir / "etag.txt"
	private val extractedDir = baseDir / "extracted"
	val repository = NEURepository.of(extractedDir)

	private fun downloadRepo(forceDownload: Boolean) {
		val downloadUrl = URI("https://github.com/notEnoughUpdates/notEnoughUpdates-REPO/archive/master.zip")
		val connection = downloadUrl.toURL().openConnection() as HttpURLConnection
		connection.setRequestProperty("If-None-Match",
		                              if (!forceDownload && Files.exists(etagFile) && Files.exists(extractedDir))
			                              etagFile.readText() else "nothing")
		val stream = connection.getInputStream()
		if (connection.responseCode == 304) {
			println("Old ETag matches - not downloading repo.")
			connection.disconnect()
			stream.close()
			return
		}
		require(connection.responseCode == 200)
		val etag = connection.getHeaderField("ETag")
		if (etag.isNotBlank()) {
			println("Received ETag. Writing.")
			etagFile.writeText(etag)
		} else {
			println("Did not receive ETag. Next startup will redownload the repo.")
			etagFile.deleteIfExists()
		}
		extractRepo(stream)
		connection.disconnect()
		stream.close()
	}

	@OptIn(ExperimentalPathApi::class)
	private fun extractRepo(inputStream: InputStream) {
		val zis = ZipInputStream(inputStream)
		extractedDir.deleteRecursively()
		while (true) {
			val entry = zis.nextEntry ?: break
			val entryPath = extractedDir.resolve(entry.name.substringAfter("/"))
			if (extractedDir !in generateSequence(entryPath) { it.parent }) error("Illegal extraction: ${entry.name}")
			if (!entry.isDirectory) {
				entryPath.createParentDirectories()
				entryPath.outputStream().use { os ->
					zis.copyTo(os)
				}
			}
		}
	}

	private val updateThread = Executors.newFixedThreadPool(1)
	fun init() {
		baseDir.createDirectories()
		updateThread.submit {
			downloadRepo(false)
			repository.reload()
		}
	}

	fun forceUpdate(): Future<*> {
		return updateThread.submit {
			downloadRepo(true)
			repository.reload()
		}
	}

	val items: ObservableList<NEUItem> = FXCollections.observableArrayList()

	init {
		repository.registerReloadListener {
			Platform.runLater {
				items.setAll(repository.items.items.values)
			}
		}
	}


}