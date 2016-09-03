package util

import java.io.File

fun File.open2(mode: String): FileStream2 = FileStream2(this, mode)
fun ByteArray.open2(mode: String): MemoryStream2 = MemoryStream2(this)

//fun File.openAsync2(): Promise<AsyncStream2> = LocalVfs()[this.absolutePath].openAsync()
