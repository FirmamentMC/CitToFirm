package moe.nea.cittofirm.studio

object Resources {
	val defaultModel = javaClass
		.getResource("/default_model.json")!!
		.readText()
}