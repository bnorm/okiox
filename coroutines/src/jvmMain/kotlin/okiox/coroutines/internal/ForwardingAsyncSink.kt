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

package okiox.coroutines.internal

import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Sink
import okiox.coroutines.AsyncSink
import kotlin.coroutines.CoroutineContext

internal class ForwardingAsyncSink(
  val delegate: Sink,
  private val context: CoroutineContext,
) : AsyncSink {
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
