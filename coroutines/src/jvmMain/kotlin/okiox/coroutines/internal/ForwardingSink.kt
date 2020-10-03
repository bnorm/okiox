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

import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Sink
import okio.Timeout
import okiox.coroutines.AsyncSink

internal class ForwardingSink(
  val delegate: AsyncSink
) : Sink {
  private val timeout = Timeout()

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
