package com.multi2.app.engine.transfer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * WiFi direct transfer — parity with the original's File Transfer
 * (same-network send/receive of apps between two Multi2 installs).
 *
 * Protocol (one connection, one payload):
 *   client → UTF token ("M2T1")
 *   server → UTF display name, LONG byte count, then zip bytes
 *
 * The QR/manual code is "<ip>:<port>".
 */
object FileTransfer {

    private const val TAG = "FileTransfer"
    private const val TOKEN = "M2T1"

    fun localIpAddress(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        }.getOrNull()
    }

    /** Serve the apk files of one app as an on-the-fly zip (.apks container). */
    suspend fun serve(
        port: Int,
        displayName: String,
        apkFiles: List<File>,
        onProgress: (sent: Long, total: Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val total = apkFiles.sumOf { it.length() }
            ServerSocket(port).use { server ->
                Log.i(TAG, "serving on $port, waiting for receiver")
                server.soTimeout = 120_000
                val socket = server.accept()
                socket.use { s ->
                    val out = DataOutputStream(s.getOutputStream().buffered())
                    val token = DataInputStream(s.getInputStream()).readUTF()
                    require(token == TOKEN) { "bad token" }
                    out.writeUTF(displayName)
                    out.writeLong(total)
                    var sent = 0L
                    ZipOutputStream(out).use { zip ->
                        apkFiles.forEach { file ->
                            zip.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { input ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    val read = input.read(buf)
                                    if (read < 0) break
                                    zip.write(buf, 0, read)
                                    sent += read
                                    onProgress(sent, total)
                                }
                            }
                            zip.closeEntry()
                        }
                        zip.flush()
                    }
                    out.flush()
                }
            }
        }
    }

    /** Receive a payload into destFile; returns display name. */
    suspend fun receive(
        ip: String,
        port: Int,
        destFile: File,
        onProgress: (received: Long, total: Long) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), 10_000)
                val input = DataInputStream(socket.getInputStream().buffered())
                DataOutputStream(socket.getOutputStream().buffered()).apply {
                    writeUTF(TOKEN)
                    flush()
                }
                val displayName = input.readUTF()
                val total = input.readLong()
                destFile.parentFile?.mkdirs()
                destFile.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var received = 0L
                    while (received < total) {
                        val read = input.read(buf, 0, minOf(buf.size.toLong(), total - received).toInt())
                        if (read < 0) break
                        out.write(buf, 0, read)
                        received += read
                        onProgress(received, total)
                    }
                }
                displayName
            }
        }
    }
}
