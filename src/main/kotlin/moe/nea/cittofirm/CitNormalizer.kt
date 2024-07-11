package moe.nea.cittofirm

import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.reader
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writer

@OptIn(ExperimentalPathApi::class)
object CitNormalizer {
	fun normalize(old: Path, normalized: Path) {
		normalized.deleteRecursively()
		normalized.createDirectories()
		old.walk()
			.filter { it.isRegularFile() }
			.forEach {
				val path = it.relativeTo(old).toString().convertPathString()
				val outputPath = normalized.resolve(path)
				outputPath.createParentDirectories()
				if (outputPath.exists()) {
					println("Encountered name collision: $outputPath. Fix your pack to not use spaces or upper case")
					return@forEach
				}
				if (path.endsWith(".properties")) {
					val prop = Properties()
					val oldProp = prop.clone() as Properties
					prop.load(it.reader())
					prop.entries.forEach {
						val key = it.key as String
						if (key.startsWith("model") || key.startsWith("texture"))
							it.setValue((it.value as String).convertPathString())
					}
					if (prop != oldProp) {
						prop.store(outputPath.writer(), "Converted by cittofirm")
						return@forEach
					}
				}
				it.copyTo(outputPath)
			}
	}

	fun String.convertPathString(): String {
		return lowercase().replace(" ", "_")
	}

}
