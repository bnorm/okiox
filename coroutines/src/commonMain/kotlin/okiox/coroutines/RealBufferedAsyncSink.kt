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
import okio.Source
import kotlin.jvm.JvmField

internal class RealBufferedAsyncSink(
  @JvmField val sink: AsyncSink
) : BufferedAsyncSink {

  @JvmField val bufferField = Buffer()
  @JvmField var closed: Boolean = false

  @Suppress("OVERRIDE_BY_INLINE") // Prevent internal code from calling the getter.
  override val buffer: Buffer
    inline get() = bufferField

  override suspend fun write(source: Buffer, byteCount: Long) {
    check(!closed) { "closed" }
    buffer.write(source, byteCount)
    emitCompleteSegments()
  }

  override suspend fun write(byteString: ByteString): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.write(byteString)
    return emitCompleteSegments()
  }

  override suspend fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeUtf8(string, beginIndex, endIndex)
    return emitCompleteSegments()
  }

  override suspend fun writeUtf8CodePoint(codePoint: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeUtf8CodePoint(codePoint)
    return emitCompleteSegments()
  }

  override suspend fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.write(source, offset, byteCount)
    return emitCompleteSegments()
  }

  override suspend fun writeAll(source: AsyncSource): Long {
    check(!closed) { "closed" }
    var totalBytesRead = 0L
    while (true) {
      val readCount: Long = source.read(buffer, SEGMENT_SIZE)
      if (readCount == -1L) break
      totalBytesRead += readCount
      emitCompleteSegments()
    }
    return totalBytesRead
  }

  override suspend fun write(source: AsyncSource, byteCount: Long): BufferedAsyncSink {
    check(!closed) { "closed" }
    var remaining = byteCount
    while (remaining > 0L) {
      val read = source.read(buffer, remaining)
      if (read == -1L) throw EOFException()
      remaining -= read
      emitCompleteSegments()
    }
    return this
  }

  override suspend fun writeByte(b: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeByte(b)
    return emitCompleteSegments()
  }

  override suspend fun writeShort(s: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeShort(s)
    return emitCompleteSegments()
  }

  override suspend fun writeShortLe(s: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeShortLe(s)
    return emitCompleteSegments()
  }

  override suspend fun writeInt(i: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeInt(i)
    return emitCompleteSegments()
  }

  override suspend fun writeIntLe(i: Int): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeIntLe(i)
    return emitCompleteSegments()
  }

  override suspend fun writeLong(v: Long): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeLong(v)
    return emitCompleteSegments()
  }

  override suspend fun writeLongLe(v: Long): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeLongLe(v)
    return emitCompleteSegments()
  }

  override suspend fun writeDecimalLong(v: Long): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeDecimalLong(v)
    return emitCompleteSegments()
  }

  override suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedAsyncSink {
    check(!closed) { "closed" }
    buffer.writeHexadecimalUnsignedLong(v)
    return emitCompleteSegments()
  }

  override suspend fun emitCompleteSegments(): BufferedAsyncSink {
    check(!closed) { "closed" }
    val byteCount = buffer.completeSegmentByteCount()
    if (byteCount > 0L) sink.write(buffer, byteCount)
    return this
  }

  override suspend fun emit(): BufferedAsyncSink {
    check(!closed) { "closed" }
    val byteCount = buffer.size
    if (byteCount > 0L) sink.write(buffer, byteCount)
    return this
  }

  override suspend fun flush() {
    check(!closed) { "closed" }
    if (buffer.size > 0L) {
      sink.write(buffer, buffer.size)
    }
    sink.flush()
  }

  override suspend fun close() {
    if (closed) return

    // Emit buffered data to the underlying sink. If this fails, we still need
    // to close the sink; otherwise we risk leaking resources.
    var thrown: Throwable? = null
    try {
      if (buffer.size > 0) {
        sink.write(buffer, buffer.size)
      }
    } catch (e: Throwable) {
      thrown = e
    }

    try {
      sink.close()
    } catch (e: Throwable) {
      if (thrown == null) thrown = e
    }

    closed = true

    if (thrown != null) throw thrown
  }

  override fun toString() = "buffer($sink)"
}
