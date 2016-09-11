package com.soywiz.vitaorganizer.i18n

import java.io.InputStreamReader
import java.io.Reader
import java.util.*
import kotlin.reflect.KProperty

object Translations {
	val classLoader = javaClass.classLoader
	val defaultProperties by lazy {
		readPropertiesFromResources("translations/en.properties")
	}
	val propertiesByLocale = hashMapOf<Locale, Properties>()

	private fun readPropertiesFromResources(path: String): Properties {
		return Properties().apply { load(InputStreamReader(classLoader.getResource(path).openStream(), Charsets.UTF_8)) }
	}

	fun getProperties(locale: Locale) = propertiesByLocale.getOrPut(locale) {
		try {
			readPropertiesFromResources("translations/${locale.language}.properties")
		} catch (e: Throwable) {
			defaultProperties
		}
	}

	fun getTranslation(locale: Locale, id: String): String {
		return getProperties(locale).getProperty(id) ?: defaultProperties.getProperty(id) ?: id
	}
}

object TextFormatter {
	fun format(text: String, args: Map<String, Any>): String {
		return text.replace(Regex("%(\\w+)%")) { r ->
			val key = r.groups[1]?.value
			args[key]?.toString() ?: r.value
		}
	}
}

class Text(val id: String) {
	fun format(vararg args: Pair<String, Any>, locale: Locale = Locale.getDefault()): String = format(args.toMap(), locale)

	fun format(args: Map<String, Any>, locale: Locale = Locale.getDefault()): String {
		return TextFormatter.format(Translations.getTranslation(locale, id), args)
	}

	override fun toString(): String = format()
}

class Translation {
	var text: Text? = null

	operator fun getValue(thisRef: Any, property: KProperty<*>): Text {
		if (text == null) text = Text(property.name)
		return text!!
	}

	//public operator fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Text): kotlin.Unit {
	//}
}
