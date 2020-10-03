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
import okiox.coroutines.internal.SEGMENT_SIZE

internal class RealBufferedAsyncSink(
  private val sink: AsyncSink
) : BufferedAsyncSink {
  private var closed: Boolean = false
  override val buffer = Buffer()

  override suspend fun write(source: Buffer, byteCount: Long) {
    emitCompleteSegments { buffer.write(source, byteCount) }
  }

  override suspend fun write(byteString: ByteString): BufferedAsyncSink =
    emitCompleteSegments { buffer.write(byteString) }

  override suspend fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeUtf8(string, beginIndex, endIndex) }

  override suspend fun writeUtf8CodePoint(codePoint: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeUtf8CodePoint(codePoint) }

  override suspend fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.write(source, offset, byteCount) }

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

  override suspend fun writeByte(b: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeByte(b) }

  override suspend fun writeShort(s: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeShort(s) }

  override suspend fun writeShortLe(s: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeShortLe(s) }

  override suspend fun writeInt(i: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeInt(i) }

  override suspend fun writeIntLe(i: Int): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeIntLe(i) }

  override suspend fun writeLong(v: Long): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeLong(v) }

  override suspend fun writeLongLe(v: Long): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeLongLe(v) }

  override suspend fun writeDecimalLong(v: Long): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeDecimalLong(v) }

  override suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedAsyncSink =
    emitCompleteSegments { buffer.writeHexadecimalUnsignedLong(v) }

  override suspend fun emitCompleteSegments(): BufferedAsyncSink =
    emitCompleteSegments {}

  private suspend inline fun emitCompleteSegments(block: () -> Unit): BufferedAsyncSink {
    check(!closed) { "closed" }
    block()
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
