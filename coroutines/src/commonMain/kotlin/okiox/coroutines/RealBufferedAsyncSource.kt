/*
 * Copyright (C) 2019 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package okiox.coroutines

import okio.Buffer
import okio.ByteString
import okio.EOFException
import okio.Options
import kotlin.jvm.JvmField

internal class RealBufferedAsyncSource(
  @JvmField val source: AsyncSource
) : BufferedAsyncSource {

  @JvmField val bufferField = Buffer()
  @JvmField var closed: Boolean = false

  @Suppress("OVERRIDE_BY_INLINE") // Prevent internal code from calling the getter.
  override val buffer: Buffer
    inline get() = bufferField

  override suspend fun read(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }

    if (buffer.size == 0L) {
      val read = source.read(buffer, SEGMENT_SIZE)
      if (read == -1L) return -1L
    }

    val toRead = minOf(byteCount, buffer.size)
    return buffer.read(sink, toRead)
  }

  override suspend fun exhausted(): Boolean {
    check(!closed) { "closed" }
    return buffer.exhausted() && source.read(buffer, SEGMENT_SIZE) == -1L
  }

  override suspend fun require(byteCount: Long) {
    if (!request(byteCount)) throw EOFException()
  }

  override suspend fun request(byteCount: Long): Boolean {
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }
    while (buffer.size < byteCount) {
      if (source.read(buffer, SEGMENT_SIZE) == -1L) return false
    }
    return true
  }

  override suspend fun readByte(): Byte {
    require(1)
    return buffer.readByte()
  }

  override suspend fun readByteString(): ByteString {
    check(!closed) { "closed" }
    buffer.writeAll(source)
    return buffer.readByteString()
  }

  override suspend fun readByteString(byteCount: Long): ByteString {
    require(byteCount)
    return buffer.readByteString(byteCount)
  }

  override suspend fun select(options: Options): Int {
    TODO()
//    check(!closed) { "closed" }
//
//    while (true) {
//      val index = buffer.selectPrefix(options, selectTruncated = true)
//      when (index) {
//        -1 -> {
//          return -1
//        }
//        -2 -> {
//          // We need to grow the buffer. Do that, then try it all again.
//          if (source.read(buffer, SEGMENT_SIZE) == -1L) return -1
//        }
//        else -> {
//          // We matched a full byte string: consume it and return it.
//          val selectedSize = options.byteStrings[index].size
//          buffer.skip(selectedSize.toLong())
//          return index
//        }
//      }
//    }
  }

  override suspend fun readByteArray(): ByteArray {
    check(!closed) { "closed" }
    buffer.writeAll(source)
    return buffer.readByteArray()
  }

  override suspend fun readByteArray(byteCount: Long): ByteArray {
    require(byteCount)
    return buffer.readByteArray(byteCount)
  }

  override suspend fun read(sink: ByteArray) = read(sink, 0, sink.size)

  override suspend fun readFully(sink: ByteArray) {
    try {
      require(sink.size.toLong())
    } catch (e: EOFException) {
      // The underlying source is exhausted. Copy the bytes we got before rethrowing.
      var offset = 0
      while (buffer.size > 0L) {
        val read = buffer.read(sink, offset, buffer.size.toInt())
        if (read == -1) throw AssertionError()
        offset += read
      }
      throw e
    }

    buffer.readFully(sink)
  }

  override suspend fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
    // TODO checkOffsetAndCount(sink.size.toLong(), offset.toLong(), byteCount.toLong())

    check(!closed) { "closed" }
    if (buffer.size == 0L) {
      val read = source.read(buffer, SEGMENT_SIZE)
      if (read == -1L) return -1
    }

    val toRead = minOf(byteCount.toLong(), buffer.size).toInt()
    return buffer.read(sink, offset, toRead)
  }

  override suspend fun readFully(sink: Buffer, byteCount: Long) {
    try {
      require(byteCount)
    } catch (e: EOFException) {
      // The underlying source is exhausted. Copy the bytes we got before rethrowing.
      sink.writeAll(buffer)
      throw e
    }

    buffer.readFully(sink, byteCount)
  }

  override suspend fun readAll(sink: AsyncSink): Long {
    check(!closed) { "closed" }
    var totalBytesWritten: Long = 0
    while (source.read(buffer, SEGMENT_SIZE) != -1L) {
      val emitByteCount = buffer.completeSegmentByteCount()
      if (emitByteCount > 0L) {
        totalBytesWritten += emitByteCount
        sink.write(buffer, emitByteCount)
      }
    }
    if (buffer.size > 0L) {
      totalBytesWritten += buffer.size
      sink.write(buffer, buffer.size)
    }
    return totalBytesWritten
  }

  override suspend fun readUtf8(): String {
    check(!closed) { "closed" }
    buffer.writeAll(source)
    return buffer.readUtf8()
  }

  override suspend fun readUtf8(byteCount: Long): String {
    require(byteCount)
    return buffer.readUtf8(byteCount)
  }

  override suspend fun readUtf8Line(): String? {
    TODO()
//    val newline = indexOf('\n'.toByte())
//
//    return if (newline == -1L) {
//      if (buffer.size != 0L) {
//        readUtf8(buffer.size)
//      } else {
//        null
//      }
//    } else {
//      buffer.readUtf8Line(newline)
//    }
  }

  override suspend fun readUtf8LineStrict() = readUtf8LineStrict(Long.MAX_VALUE)

  override suspend fun readUtf8LineStrict(limit: Long): String {
    TODO()
//    require(limit >= 0) { "limit < 0: $limit" }
//    val scanLength = if (limit == Long.MAX_VALUE) Long.MAX_VALUE else limit + 1
//    val newline = indexOf('\n'.toByte(), 0, scanLength)
//    if (newline != -1L) return buffer.readUtf8Line(newline)
//    if (scanLength < Long.MAX_VALUE &&
//        request(scanLength) && buffer[scanLength - 1] == '\r'.toByte() &&
//        request(scanLength + 1) && buffer[scanLength] == '\n'.toByte()) {
//      return buffer.readUtf8Line(scanLength) // The line was 'limit' UTF-8 bytes followed by \r\n.
//    }
//    val data = Buffer()
//    buffer.copyTo(data, 0, minOf(32, buffer.size))
//    throw EOFException("\\n not found: limit=" + minOf(buffer.size, limit) +
//      " content=" + data.readByteString().hex() + '…'.toString())
  }

  override suspend fun readUtf8CodePoint(): Int {
    require(1)

    val b0 = buffer[0].toInt()
    when {
      b0 and 0xe0 == 0xc0 -> require(2)
      b0 and 0xf0 == 0xe0 -> require(3)
      b0 and 0xf8 == 0xf0 -> require(4)
    }

    return buffer.readUtf8CodePoint()
  }

  override suspend fun readShort(): Short {
    require(2)
    return buffer.readShort()
  }

  override suspend fun readShortLe(): Short {
    require(2)
    return buffer.readShortLe()
  }

  override suspend fun readInt(): Int {
    require(4)
    return buffer.readInt()
  }

  override suspend fun readIntLe(): Int {
    require(4)
    return buffer.readIntLe()
  }

  override suspend fun readLong(): Long {
    require(8)
    return buffer.readLong()
  }

  override suspend fun readLongLe(): Long {
    require(8)
    return buffer.readLongLe()
  }

  override suspend fun readDecimalLong(): Long {
    require(1)

    var pos = 0L
    while (request(pos + 1)) {
      val b = buffer[pos]
      if ((b < '0'.toByte() || b > '9'.toByte()) && (pos != 0L || b != '-'.toByte())) {
        // Non-digit, or non-leading negative sign.
        if (pos == 0L) {
          throw NumberFormatException("Expected leading [0-9] or '-' character but was 0x${b.toString(16)}")
        }
        break
      }
      pos++
    }

    return buffer.readDecimalLong()
  }

  override suspend fun readHexadecimalUnsignedLong(): Long {
    require(1)

    var pos = 0
    while (request((pos + 1).toLong())) {
      val b = buffer[pos.toLong()]
      if ((b < '0'.toByte() || b > '9'.toByte()) &&
          (b < 'a'.toByte() || b > 'f'.toByte()) &&
          (b < 'A'.toByte() || b > 'F'.toByte())) {
        // Non-digit, or non-leading negative sign.
        if (pos == 0) {
          throw NumberFormatException("Expected leading [0-9a-fA-F] character but was 0x${b.toString(16)}")
        }
        break
      }
      pos++
    }

    return buffer.readHexadecimalUnsignedLong()
  }

  override suspend fun skip(byteCount: Long) {
    var remaining = byteCount
    check(!closed) { "closed" }
    while (remaining > 0) {
      if (buffer.size == 0L && source.read(buffer, SEGMENT_SIZE) == -1L) {
        throw EOFException()
      }
      val toSkip = minOf(remaining, buffer.size)
      buffer.skip(toSkip)
      remaining -= toSkip
    }
  }

  override suspend fun indexOf(b: Byte, fromIndex: Long, toIndex: Long): Long {
    var fromIndex = fromIndex
    check(!closed) { "closed" }
    require(fromIndex in 0L..toIndex) { "fromIndex=$fromIndex toIndex=$toIndex" }

    while (fromIndex < toIndex) {
      val result = buffer.indexOf(b, fromIndex, toIndex)
      if (result != -1L) return result

      // The byte wasn't in the buffer. Give up if we've already reached our target size or if the
      // underlying stream is exhausted.
      val lastBufferSize = buffer.size
      if (lastBufferSize >= toIndex || source.read(buffer, SEGMENT_SIZE) == -1L) return -1L

      // Continue the search from where we left off.
      fromIndex = maxOf(fromIndex, lastBufferSize)
    }
    return -1L
  }

  override suspend fun indexOf(bytes: ByteString, fromIndex: Long): Long {
    var fromIndex = fromIndex
    check(!closed) { "closed" }

    while (true) {
      val result = buffer.indexOf(bytes, fromIndex)
      if (result != -1L) return result

      val lastBufferSize = buffer.size
      if (source.read(buffer, SEGMENT_SIZE) == -1L) return -1L

      // Keep searching, picking up from where we left off.
      fromIndex = maxOf(fromIndex, lastBufferSize - bytes.size + 1)
    }
  }

  override suspend fun indexOfElement(targetBytes: ByteString, fromIndex: Long): Long {
    var fromIndex = fromIndex
    check(!closed) { "closed" }

    while (true) {
      val result = buffer.indexOfElement(targetBytes, fromIndex)
      if (result != -1L) return result

      val lastBufferSize = buffer.size
      if (source.read(buffer, SEGMENT_SIZE) == -1L) return -1L

      // Keep searching, picking up from where we left off.
      fromIndex = maxOf(fromIndex, lastBufferSize)
    }
  }

  override suspend fun rangeEquals(
    offset: Long,
    bytes: ByteString,
    bytesOffset: Int,
    byteCount: Int
  ): Boolean {
    check(!closed) { "closed" }

    if (offset < 0L ||
        bytesOffset < 0 ||
        byteCount < 0 ||
        bytes.size - bytesOffset < byteCount) {
      return false
    }
    for (i in 0 until byteCount) {
      val bufferOffset = offset + i
      if (!request(bufferOffset + 1)) return false
      if (buffer[bufferOffset] != bytes[bytesOffset + i]) return false
    }
    return true
  }

  override fun peek(): BufferedAsyncSource = TODO()

  override suspend fun close() {
    if (closed) return
    closed = true
    source.close()
    buffer.clear()
  }

  override fun toString() = "buffer($source)"
}
