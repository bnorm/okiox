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
