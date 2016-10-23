package com.soywiz.vitaorganizer.ext

import java.awt.Component
import java.awt.Container

inline fun <reified T : Any> Component.findFirstByClass(): T? {
	return this.findFirstByClass(T::class.java)
}

fun <T> Component.findFirstByClass(clazz: Class<T>): T? {
	if (this.javaClass.isAssignableFrom(clazz)) return this as T

	if (this is Container) {
		//for (component in this.components) {
		//	if (component.javaClass.isAssignableFrom(clazz)) return component as T
		//}

		for (component in this.components) {
			val result = component.findFirstByClass(clazz)
			if (result != null) return result
		}
	}

	return null
}