package moe.nea.cittofirm.studio

interface LintError {
	val severity: Severity

	enum class Severity {
		WARNING,
		ERROR,
	}
}
