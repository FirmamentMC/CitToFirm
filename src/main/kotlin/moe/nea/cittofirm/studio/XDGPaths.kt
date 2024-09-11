package moe.nea.cittofirm.studio

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

object XDGPaths {
	private fun getPOpt(name: String) = System.getProperty(name)?.let(::Path)
	private fun getEOpt(name: String, init: (Path) -> Path = { it }) = System.getenv(name)?.let(::Path)?.let(init)
	fun home() = Path(System.getProperty("user.home"))
	fun config() = getEOpt("XDG_CONFIG_HOME") ?: (home() / ".config")
	fun cache() = getEOpt("XDG_CACHE_HOME") ?: (home() / ".cache")
	fun data() = getEOpt("XDG_DATA_HOME") ?: (home() / ".local/share")
	fun state() = getEOpt("XDG_STATE_HOME") ?: (home() / ".local/state")

}