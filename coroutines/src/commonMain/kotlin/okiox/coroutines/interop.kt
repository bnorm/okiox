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

@file:JvmName("-Interop")

package okiox.coroutines

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.EOFException
import okio.Sink
import okio.Source
import okiox.coroutines.internal.SEGMENT_SIZE
import kotlin.jvm.JvmName

/**
 * Removes all bytes from this and appends them to `sink`. Returns the total number of bytes
 * written to `sink` which will be 0 if this is exhausted.
 */
suspend fun Buffer.readAll(sink: AsyncSink): Long {
  val size = this.size
  sink.write(this, size)
  return size
}

/**
 * Removes all bytes from this and appends them to `sink`. Returns the total number of bytes
 * written to `sink` which will be 0 if this is exhausted.
 */
suspend fun BufferedSource.readAll(sink: AsyncSink): Long {
  var totalBytesWritten: Long = 0
  while (read(buffer, SEGMENT_SIZE) != -1L) {
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

/**
 * Removes all bytes from `source` and appends them to this sink. Returns the number of bytes read
 * which will be 0 if `source` is exhausted.
 */
suspend fun BufferedSink.writeAll(source: AsyncSource): Long {
  var totalBytesRead = 0L
  while (true) {
    val readCount: Long = source.read(buffer, SEGMENT_SIZE)
    if (readCount == -1L) break
    totalBytesRead += readCount
    emitCompleteSegments()
  }
  return totalBytesRead
}

/** Removes `byteCount` bytes from `source` and appends them to this sink. */
suspend fun BufferedSink.write(source: AsyncSource, byteCount: Long): BufferedSink {
  var remaining = byteCount
  while (remaining > 0L) {
    val read = source.read(buffer, remaining)
    if (read == -1L) throw EOFException()
    remaining -= read
    emitCompleteSegments()
  }
  return this
}

/**
 * Removes all bytes from this and appends them to `sink`. Returns the total number of bytes
 * written to `sink` which will be 0 if this is exhausted.
 */
suspend fun BufferedAsyncSource.readAll(sink: Sink): Long {
  var totalBytesWritten: Long = 0
  while (read(buffer, SEGMENT_SIZE) != -1L) {
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

/**
 * Removes all bytes from `source` and appends them to this sink. Returns the number of bytes read
 * which will be 0 if `source` is exhausted.
 */
suspend fun BufferedAsyncSink.writeAll(source: Source): Long {
  var totalBytesRead = 0L
  while (true) {
    val readCount: Long = source.read(buffer, SEGMENT_SIZE)
    if (readCount == -1L) break
    totalBytesRead += readCount
    emitCompleteSegments()
  }
  return totalBytesRead
}

/** Removes `byteCount` bytes from `source` and appends them to this sink. */
suspend fun BufferedAsyncSink.write(source: Source, byteCount: Long): BufferedAsyncSink {
  var remaining = byteCount
  while (remaining > 0L) {
    val read = source.read(buffer, remaining)
    if (read == -1L) throw EOFException()
    remaining -= read
    emitCompleteSegments()
  }
  return this
}
