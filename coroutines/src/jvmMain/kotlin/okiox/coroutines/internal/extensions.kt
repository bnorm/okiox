/*
 * Copyright (C) 2020 Brian Norman
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

@file:JvmName("-JvmInternalExtensions")

package okiox.coroutines.internal

import okio.Buffer
import okio.Timeout

internal inline fun <R> Buffer.readUnsafe(
  cursor: Buffer.UnsafeCursor = Buffer.UnsafeCursor(),
  block: (cursor: Buffer.UnsafeCursor) -> R
): R {
  return readUnsafe(cursor).use {
    block(cursor)
  }
}

internal inline fun <R> Buffer.readAndWriteUnsafe(
  cursor: Buffer.UnsafeCursor = Buffer.UnsafeCursor(),
  block: (cursor: Buffer.UnsafeCursor) -> R
): R {
  return readAndWriteUnsafe(cursor).use {
    block(cursor)
  }
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

  return kotlinx.coroutines.withTimeout((waitNanos / 1_000_000f).toLong()) {
    block()
  }
}
