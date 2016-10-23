package com.soywiz.vitaorganizer.ext

import java.util.*

// Kotlin doesn't allow static method extensions yet

//inline fun Locale.Companion.setTemporarily(locale: Locale, callback: () -> Unit) {
//	val oldDefault = Locale.getDefault()
//	try {
//		Locale.setDefault(locale)
//		callback()
//	} finally {
//		Locale.setDefault(oldDefault)
//	}
//}

inline fun Locale.setTemporarily(callback: () -> Unit) {
	val oldDefault = Locale.getDefault()
	try {
		Locale.setDefault(this)
		callback()
	} finally {
		Locale.setDefault(oldDefault)
	}
}