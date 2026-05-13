package com.roadmate.core.sync.protocol

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

class MessageSerializer @Inject constructor() {

    companion object {
        /**
         * Maximum allowed message payload size (10 MB).
         * Prevents OOM from corrupted or malicious length headers.
         */
        const val MAX_MESSAGE_SIZE = 10 * 1024 * 1024
    }

    fun writeMessage(output: OutputStream, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        output.write(ByteBuffer.allocate(4).putInt(bytes.size).array())
        output.write(bytes)
        output.flush()
    }

    fun readMessage(input: InputStream): String {
        val lengthBytes = ByteArray(4)
        input.readFully(lengthBytes)
        val length = ByteBuffer.wrap(lengthBytes).int
        if (length < 0) {
            throw IOException("Invalid message length: $length")
        }
        if (length > MAX_MESSAGE_SIZE) {
            throw IOException("Message too large: $length bytes (max $MAX_MESSAGE_SIZE)")
        }
        val payload = ByteArray(length)
        input.readFully(payload)
        return String(payload, Charsets.UTF_8)
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) throw java.io.EOFException("Unexpected end of stream")
            offset += read
        }
    }
}
