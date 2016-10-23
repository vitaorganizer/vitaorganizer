package com.soywiz.vitaorganizer.ext

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset

fun runCmd(vararg args: String): ProcessResult = runCmd(args.toList())
fun runCmd(args: List<String>): ProcessResult = Runtime.getRuntime().exec(args.toTypedArray()).waitAndGetOutput()

val Process.isAliveJava6: Boolean get() {
	try {
		exitValue()
		return false
	} catch (e: IllegalThreadStateException) {
		return true
	}

}

fun Process.waitAndGetOutput(): ProcessResult {
	val outArray = ByteArrayOutputStream()
	val errArray = ByteArrayOutputStream()

	val out = this.inputStream
	val err = this.errorStream

	while (this.isAliveJava6) {
		outArray.write(out.readAvailable())
		errArray.write(err.readAvailable())
		Thread.sleep(1L)
	}

	val exitCode = this.waitFor()
	// Probably these two lines are not needed!
	outArray.write(out.readAvailable())
	errArray.write(err.readAvailable())
	return ProcessResult(outArray.toByteArray(), errArray.toByteArray(), exitCode)
}

fun InputStream.readAvailable(): ByteArray {
	val available = this.available()
	if (available == 0) return byteArrayOf()
	val data = ByteArray(available)
	val read = this.read(data)
	return data.sliceArray(0 until read)
}

class ProcessResult(val outputBytes: ByteArray, val errorBytes: ByteArray, val exitCode: Int) {
	val output = outputBytes.toString(Charset.defaultCharset())
	val error = errorBytes.toString(Charset.defaultCharset())
	val outputError = output + error
	val success: Boolean get() = exitCode == 0
}