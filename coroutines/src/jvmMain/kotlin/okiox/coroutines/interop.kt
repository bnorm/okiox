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

@file:JvmName("-JvmInterop")

package okiox.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import kotlin.coroutines.CoroutineContext

fun Sink.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSink {
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

fun AsyncSink.toBlocking(): Sink {
  class ForwardingSink(val delegate: AsyncSink) : Sink {
    val timeout = Timeout()

    override fun write(source: Buffer, byteCount: Long) = runBlocking {
      withTimeout(timeout) {
        delegate.write(source, byteCount)
      }
    }

    override fun flush() = runBlocking {
      withTimeout(timeout) {
        delegate.flush()
      }
    }

    override fun close() = runBlocking {
      withTimeout(timeout) {
        delegate.close()
      }
    }

    override fun timeout() = timeout
  }
  return ForwardingSink(this)
}

fun Source.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSource {
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

fun AsyncSource.toBlocking(): Source {
  class ForwardingSource(val delegate: AsyncSource) : Source {
    val timeout = Timeout()

    override fun read(sink: Buffer, byteCount: Long) = runBlocking {
      withTimeout(timeout) {
        delegate.read(sink, byteCount)
      }
    }

    override fun close() = runBlocking {
      withTimeout(timeout) {
        delegate.close()
      }
    }

    override fun timeout() = timeout
  }
  return ForwardingSource(this)
}

internal suspend inline fun <R> withTimeout(timeout: Timeout, crossinline block: suspend () -> R): R {
  if (timeout.timeoutNanos() == 0L && !timeout.hasDeadline()) {
    return block()
  }

  val now = System.nanoTime()
  val waitNanos = when {
    // Compute the earliest event; either timeout or deadline. Because nanoTime can wrap
    // around, minOf() is undefined for absolute values, but meaningful for relative ones.
    timeout.timeoutNanos() != 0L && timeout.hasDeadline() -> minOf(timeout.timeoutNanos(), timeout.deadlineNanoTime() - now)
    timeout.timeoutNanos() != 0L -> timeout.timeoutNanos()
    timeout.hasDeadline() -> timeout.deadlineNanoTime() - now
    else -> throw AssertionError()
  }

  return withTimeout((waitNanos / 1_000_000f).toLong()) {
    block()
  }
}
