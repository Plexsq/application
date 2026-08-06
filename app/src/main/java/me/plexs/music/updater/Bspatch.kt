package me.plexs.music.updater

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Pure-Java implementation of the classic bspatch algorithm (Colin Percival's
 * bsdiff format, "BSDIFF40", bzip2-compressed streams). Applies a patch to a
 * source file producing a byte-identical target file. Used for in-app delta
 * updates so normal releases only download a small patch instead of the full APK.
 */
object Bspatch {

    private const val MAGIC = "BSDIFF40"

    /** Apply [patch] to [old] and write the result to [out]. */
    fun apply(old: File, patch: File, out: File) {
        patch.inputStream().use { pin ->
            val reader = BufferedReader(pin)
            val header = ByteArray(32)
            reader.readFully(header)

            val magicStr = String(header, 0, 8, Charsets.US_ASCII)
            if (magicStr != MAGIC) throw IOException("Not a BSDIFF patch")

            val ctrlLen = be64(header, 8)
            val diffLen = be64(header, 16)
            val newSize = be64(header, 24)

            val ctrlStart = 32L
            val diffStart = ctrlStart + ctrlLen
            val extraStart = diffStart + diffLen

            val oldSize = old.length()
            val oldBytes = old.readBytes()

            val newBytes = ByteArray(newSize.toInt())

            var ctrl: InputStream = streamAt(patch, ctrlStart)
            var diff = streamAt(patch, diffStart)
            val extra = streamAt(patch, extraStart)

            val ctrlIn = BZip2CompressorInputStream(ctrl)
            val diffIn = BZip2CompressorInputStream(diff)
            val extraIn = BZip2CompressorInputStream(extra)

            var oldPos = 0L
            var newPos = 0L
            val newSizeInt = newSize.toInt()

            val tri = IntArray(3)
            while (newPos < newSize) {
                readTri(ctrlIn, tri)
                val x = tri[0]
                val y = tri[1]
                val z = tri[2]

                // Diff: newbyte = old[oldPos+i] + diffbyte
                for (i in 0 until x) {
                    val d = diffIn.read()
                    if (d < 0) throw IOException("diff stream truncated")
                    val base = if (i + oldPos < oldSize) oldBytes[(i + oldPos).toInt()] else 0
                    newBytes[(newPos + i).toInt()] = (base.toInt() + d).toByte()
                }
                newPos += x
                oldPos += x

                // Seek
                oldPos += y

                // Extra: copy z bytes verbatim
                var remaining = z
                var off = newPos.toInt()
                while (remaining > 0) {
                    val n = extraIn.read(newBytes, off, remaining)
                    if (n <= 0) throw IOException("extra stream truncated")
                    off += n
                    remaining -= n
                }
                newPos += z
                oldPos += z
            }

            diffIn.close()
            extraIn.close()
            ctrlIn.close()

            out.outputStream().use { it.write(newBytes) }
        }
    }

    private fun readTri(input: InputStream, tri: IntArray) {
        for (i in 0 until 3) {
            val b = ByteArray(8)
            var got = 0
            while (got < 8) {
                val n = input.read(b, got, 8 - got)
                if (n < 0) throw IOException("ctrl stream truncated")
                got += n
            }
            tri[i] = be64(b, 0).toInt()
        }
    }

    private fun be64(b: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 0 until 8) {
            v = (v shl 8) or (b[off + i].toLong() and 0xff)
        }
        return v
    }

    private fun streamAt(file: File, start: Long): InputStream {
        val m = java.nio.channels.FileChannel.open(File(file.path).toPath()).position(start)
        return java.nio.channels.Channels.newInputStream(m)
    }

    private class BufferedReader(private val in: InputStream) {
        private val buf = java.io.ByteArrayOutputStream()
        fun readFully(b: ByteArray) {
            var got = 0
            while (got < b.size) {
                val n = in.read(b, got, b.size - got)
                if (n < 0) throw IOException("patch header truncated")
                got += n
            }
        }
    }
}