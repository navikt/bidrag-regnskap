package no.nav.bidrag.regnskap.util

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ByteArrayOutputStreamTilByteBuffer : ByteArrayOutputStream() {
    fun toByteBuffer(): ByteBuffer {
        return ByteBuffer.wrap(buf, 0, count)
    }
}
