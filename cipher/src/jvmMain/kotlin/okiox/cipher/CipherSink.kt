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

package okiox.cipher

import okio.Buffer
import okio.BufferedSink
import okio.Sink
import okio.Timeout
import okio.buffer
import java.net.ProtocolException
import javax.crypto.Cipher

class CipherSink private constructor(
  private val sink: BufferedSink,
  private val cipher: Cipher
) : Sink {
  constructor(sink: Sink, cipher: Cipher) : this(sink.buffer(), cipher)

  init {
    require(cipher.algorithm.contains("NoPadding")) { cipher.algorithm }
  }

  private val sourceCursor = Buffer.UnsafeCursor()
  private val sinkCursor = Buffer.UnsafeCursor()

  private var closed: Boolean = false
  override fun write(source: Buffer, byteCount: Long) {
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }

    process(
      cipher = cipher,
      source = source,
      sourceCursor = sourceCursor,
      byteCount = byteCount,
      sink = sink.buffer,
      sinkCursor = sinkCursor
    )
    sink.emitCompleteSegments()
  }
  override fun flush() {
    check(!closed) { "closed" }
    sink.flush()
  }
  override fun close() {
    if (closed) return

    try {
      if (cipher.getOutputSize(0) > 0) {
        throw ProtocolException("blockSize=${cipher.blockSize} unprocessed=${cipher.getOutputSize(0)}")
      }
    } finally {
      sink.close()
      closed = true
    }
  }

  override fun timeout(): Timeout = sink.timeout()
}
