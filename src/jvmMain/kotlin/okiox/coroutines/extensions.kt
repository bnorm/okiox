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

@file:JvmName("-ExtensionsJvm")
package okiox.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import kotlin.coroutines.CoroutineContext

fun Sink.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSink {
  // TODO timeout
  class ForwardingAsyncSink(val delegate: Sink) : AsyncSink {
    override suspend fun write(source: Buffer, byteCount: Long) = withContext(context) {
      delegate.write(source, byteCount)
    }

    override suspend fun flush() = withContext(context) {
      delegate.flush()
    }

    override suspend fun close() = withContext(context) {
      delegate.close()
    }
  }
  return ForwardingAsyncSink(this)
}

fun AsyncSink.toBlocking(timeout: Timeout = Timeout()): Sink {
  class ForwardingSink(val delegate: AsyncSink) : Sink {
    override fun write(source: Buffer, byteCount: Long) = runBlocking {
      delegate.write(source, byteCount)
    }

    override fun flush() = runBlocking {
      delegate.flush()
    }

    override fun timeout() = timeout

    override fun close() = runBlocking {
      delegate.close()
    }
  }
  return ForwardingSink(this)
}

fun Source.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSource {
  // TODO timeout
  class ForwardingAsyncSource(val delegate: Source) : AsyncSource {
    override suspend fun read(sink: Buffer, byteCount: Long) = withContext(context) {
      delegate.read(sink, byteCount)
    }

    override suspend fun close() = withContext(context) {
      delegate.close()
    }
  }
  return ForwardingAsyncSource(this)
}

fun AsyncSource.toBlocking(timeout: Timeout = Timeout()): Source {
  class ForwardingSource(val delegate: AsyncSource) : Source {
    override fun read(sink: Buffer, byteCount: Long) = runBlocking {
      delegate.read(sink, byteCount)
    }

    override fun timeout() = timeout

    override fun close() = runBlocking {
      delegate.close()
    }
  }
  return ForwardingSource(this)
}
