package com.soywiz.vitaorganizer

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.*

object TextFormatter {
	@JvmStatic fun format(text: String, vararg pairs: Pair<String, Any>) = format(text, pairs.toMap())
	@JvmStatic fun format(text: String, args: Map<String, Any>): String {
		return text.replace(Regex("%(\\w+)%")) { r ->
			val key = r.groups[1]?.value
			args[key]?.toString() ?: r.value
		}
	}
}
