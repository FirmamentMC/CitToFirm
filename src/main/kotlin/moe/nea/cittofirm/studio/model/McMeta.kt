package moe.nea.cittofirm.studio.model

import com.google.gson.annotations.SerializedName
import moe.nea.cittofirm.studio.gson
import java.io.File
import java.util.zip.ZipInputStream

@JvmRecord
data class McMeta(
	val pack: Pack = Pack(),
) {
	@JvmRecord
	data class Pack(
		@SerializedName("pack_format")
		val packFormat: Int = 0,
		val description: String = "",
	)

	companion object {
		fun getMeta(zipfile: File): McMeta? {
			try {
				ZipInputStream(zipfile.inputStream()).use { zis ->
					generateSequence { zis.nextEntry }.find { it.name == "pack.mcmeta" } ?: return null
					val mcMeta = gson.fromJson(zis.reader(), McMeta::class.java)
					return mcMeta
				}
			} catch (ex: Exception) {
				ex.printStackTrace()
				return null
			}
		}
	}
}