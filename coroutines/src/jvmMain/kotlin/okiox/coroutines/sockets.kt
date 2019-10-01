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

@file:JvmName("-SocketsJvm")
@file:Suppress("BlockingMethodInNonBlockingContext")

package okiox.coroutines

import okio.Buffer
import okiox.coroutines.internal.await
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

suspend fun Socket.source(): AsyncSource {
  val channel = this.channel!!

  return object : AsyncSource {
    val buffer = ByteBuffer.allocateDirect(8192)

    override suspend fun read(sink: Buffer, byteCount: Long): Long {
      channel.await(SelectionKey.OP_READ)

      buffer.clear()
      buffer.limit(minOf(buffer.capacity(), byteCount.toInt()))
      val read = channel.read(buffer)
      if (read > 0) sink.write(buffer.flip())
      return read.toLong()
    }

    override suspend fun close() {
      channel.close()
    }
  }
}

suspend fun Socket.sink(): AsyncSink {
  val channel = this.channel!!

  return object : AsyncSink {
    val cursor = Buffer.UnsafeCursor()

    override suspend fun write(source: Buffer, byteCount: Long) {
      channel.await(SelectionKey.OP_WRITE)

      source.readUnsafe(cursor).use {
        var remaining = byteCount
        while (remaining > 0) {
          cursor.seek(0)
          val length = minOf(cursor.end - cursor.start, remaining.toInt())

          val written = channel.write(ByteBuffer.wrap(cursor.data, cursor.start, length))
          if (written == 0) channel.await(SelectionKey.OP_WRITE)

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
