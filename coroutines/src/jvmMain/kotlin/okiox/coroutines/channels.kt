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

@file:JvmName("-ChannelsJvm")
@file:Suppress("BlockingMethodInNonBlockingContext")

package okiox.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okiox.coroutines.internal.SEGMENT_SIZE
import okiox.coroutines.internal.readUnsafe
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun AsynchronousSocketChannel.asAsyncSource(): AsyncSource {
  val channel = this

  return object : AsyncSource {
    val buffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    override suspend fun read(sink: Buffer, byteCount: Long): Long {
      buffer.clear()
      buffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())
      val read = channel.aRead(buffer)
      buffer.flip()
      if (read > 0) sink.write(buffer)
      return read.toLong()
    }

    override suspend fun close() {
      channel.close()
    }
  }
}

suspend fun AsynchronousSocketChannel.asAsyncSink(): AsyncSink {
  val channel = this

  return object : AsyncSink {
    val cursor = Buffer.UnsafeCursor()

    override suspend fun write(source: Buffer, byteCount: Long) {
      source.readUnsafe(cursor) {
        var remaining = byteCount
        while (remaining > 0) {
          cursor.seek(0)
          val length = minOf(cursor.end - cursor.start, remaining.toInt())

          val written = aWrite(ByteBuffer.wrap(cursor.data, cursor.start, length))
          remaining -= written
          source.skip(written.toLong())
        }
      }
    }

    override suspend fun flush() {
    }

    override suspend fun close() {
      channel.close()
    }
  }
}

suspend fun AsynchronousFileChannel.asAsyncSource(): AsyncSource {
  val channel = this

  return object : AsyncSource {
    var position = 0L
    val buffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    override suspend fun read(sink: Buffer, byteCount: Long): Long {
      buffer.clear()
      buffer.limit(minOf(buffer.capacity(), byteCount.toInt()))
      val read = channel.aRead(buffer, position)
      if (read > 0) {
        position += read
        buffer.flip()
        sink.write(buffer)
      }
      return read.toLong()
    }

    override suspend fun close() {
      channel.close()
    }
  }
}

suspend fun AsynchronousFileChannel.asAsyncSink(): AsyncSink {
  val channel = this

  return object : AsyncSink {
    var position = 0L
    val cursor = Buffer.UnsafeCursor()

    override suspend fun write(source: Buffer, byteCount: Long) {
      source.readUnsafe(cursor) {
        var remaining = byteCount
        while (remaining > 0) {
          cursor.seek(0)
          val length = minOf(cursor.end - cursor.start, remaining.toInt())

          val written = aWrite(ByteBuffer.wrap(cursor.data, cursor.start, length), position)
          remaining -= written
          position += written
          source.skip(written.toLong())
        }
      }
    }

    override suspend fun flush() {
      channel.force(/* metaData */ false)
    }

    override suspend fun close() {
      channel.close()
    }
  }
}

private suspend fun AsynchronousSocketChannel.aRead(buffer: ByteBuffer): Int = suspendCancellableCoroutine { cont ->
  read(buffer, cont, ChannelCompletionHandler)
  cont.invokeOnCancellation { close() }
}

private suspend fun AsynchronousSocketChannel.aWrite(buffer: ByteBuffer): Int = suspendCancellableCoroutine { cont ->
  write(buffer, cont, ChannelCompletionHandler)
  cont.invokeOnCancellation { close() }
}

private suspend fun AsynchronousFileChannel.aRead(buffer: ByteBuffer, position: Long): Int = suspendCancellableCoroutine { cont ->
  read(buffer, position, cont, ChannelCompletionHandler)
  cont.invokeOnCancellation { close() }
}

private suspend fun AsynchronousFileChannel.aWrite(buffer: ByteBuffer, position: Long): Int = suspendCancellableCoroutine { cont ->
  write(buffer, position, cont, ChannelCompletionHandler)
  cont.invokeOnCancellation { close() }
}

private object ChannelCompletionHandler : CompletionHandler<Int, CancellableContinuation<Int>> {
  override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
    attachment.resume(result)
  }

  override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
    attachment.resumeWithException(exc)
  }
}
