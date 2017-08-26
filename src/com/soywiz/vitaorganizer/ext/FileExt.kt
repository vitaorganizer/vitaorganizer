package com.soywiz.vitaorganizer.ext

import java.io.File

fun File.listdirRecursively(): List<File> {
	val out = arrayListOf<File>()
	this.listdirRecursively { out += it }
	return out
}

fun File.listdirRecursively(emit: (file: File) -> Unit) {
	for (child in this.listFiles()) {
		emit(child)
		if (child.isDirectory) {
			child.listdirRecursively(emit)
		}
	}
}

fun File.safe_exists() : Boolean {
	try {
		when {
			path != null && path.isNotEmpty() -> return exists()
			else -> return false
		}
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_isFile() : Boolean {
	if(!safe_exists())
		return false

	try {
		return isFile()
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_isDirectory() : Boolean {
	if(!safe_exists())
		return false

	try {
		return isDirectory()
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_canRead() : Boolean {
	if(!safe_exists())
		return false

	try {
		return canRead()
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_canWrite() : Boolean {
	if(!safe_exists())
		return false

	try {
		return canWrite()
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_canReadWrite() : Boolean {
	if(!safe_exists())
		return false

	try {
		return canRead() && canWrite()
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_canExecute() : Boolean {
	if(!safe_exists())
		return false

	try {
		return canExecute();
	}
	catch(e: Throwable) {
		return false
	}
}

fun File.safe_canDelete() : Boolean = safe_canReadWrite()

fun File.safe_delete() : Boolean {
	if(!safe_canDelete())
		return false

	try {
		if(isDirectory())
			return deleteRecursively()
		else if(isFile())
			return delete()
	}
	catch(e: Throwable) {
	}
	return false
}

fun File.createAndCheckFile() : Boolean {
	try {
		if(createNewFile())
			if(canWrite())
				return true;
	}
	catch(e: Throwable) {
	}
	return false
}

fun File.listAllFiles(ext: String? = null): MutableList<File> {
	val mutableList : MutableList<File> = arrayListOf()
	for (en in listFiles()) {
		if(ext != null && en.extension != ext && !en.isDirectory)
			continue
		else
			mutableList.add(en)

		if(en.isDirectory)
			mutableList.addAll(listAllFiles(ext))
	}
	return mutableList;
}
