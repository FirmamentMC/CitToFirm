package moe.nea.cittofirm.studio

import java.io.Closeable
import java.nio.file.ClosedWatchServiceException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.isDirectory

class RecursiveWatchService(
	val root: Path,
	val pollDebounce: Duration,
	val dispatch: (Set<FileChange>) -> Unit,
) : Closeable {
	private val watcher = root.fileSystem.newWatchService()
	private val pollThread = thread(start = true) {
		try {
			pollLoop()
		} catch (ex: ClosedWatchServiceException) {
			// pass
		}
	}

	private fun enterDirectory(path: Path) {
		Files.walk(path).filter { it.isDirectory() }
			.forEach {
				it.register(watcher,
				              StandardWatchEventKinds.ENTRY_CREATE,
				              StandardWatchEventKinds.ENTRY_DELETE,
				              StandardWatchEventKinds.ENTRY_MODIFY)
			}
	}

	private fun pollLoop() {
		while (true) {
			val key = watcher.take()
			processKey(key)
			while (true) {
				println("Checking for additional keys")
				val pollKey = watcher.poll(pollDebounce.toMillis(), TimeUnit.MILLISECONDS) ?: break
				processKey(pollKey)
			}
			println("No more additional keys. Dispatching events.")
			dispatchEvents()
		}
	}

	private fun processKey(key: WatchKey) {
		processEvents(key, key.pollEvents())
		val valid = key.reset()
		if (!valid) {
			println("No longer watching $key")
		}
	}


	enum class FileChangeKind {
		CREATED,
		MODIFY,
		DELETED,
	}

	data class FileChange(
		val file: Path, val type: FileChangeKind,
	)

	private var events: MutableSet<FileChange> = mutableSetOf()

	private fun dispatchEvents() {
		val oldEvents = events
		dispatch(oldEvents)
		events = mutableSetOf()
	}

	private fun processEvents(watchKey: WatchKey, pollEvents: List<WatchEvent<*>>) {
		println("Received fs events")
		val root = watchKey.watchable() as Path
		for (pollEvent in pollEvents) {
			val resolvedPath = root.resolve(pollEvent.context() as Path)
			// TODO: make create and delete events remove each other
			if (pollEvent.kind() == StandardWatchEventKinds.OVERFLOW) {
				// TODO: handle overflow (somehow)
			} else if (pollEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
				if (resolvedPath.isDirectory())
					enterDirectory(resolvedPath)
				events.add(FileChange(resolvedPath, FileChangeKind.CREATED))
			} else if (pollEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
				events.add(FileChange(resolvedPath, FileChangeKind.DELETED))
			} else if (pollEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
				events.add(FileChange(resolvedPath, FileChangeKind.MODIFY))
			}
		}
	}

	init {
		enterDirectory(root)
	}
	override fun close() {
		watcher.close()
		pollThread.interrupt()
	}
}