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
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import java.io.IOException
import java.net.ProtocolException
import javax.crypto.Cipher

class CipherSource private constructor(
  private val source: BufferedSource,
  private val cipher: Cipher
) : Source {
  constructor(source: Source, cipher: Cipher) : this(source.buffer(), cipher) {}

  init {
    require(cipher.algorithm.contains("NoPadding")) { cipher.algorithm }
  }

  private val ciphered = Buffer()
  private val sourceCursor = Buffer.UnsafeCursor()
  private val sinkCursor = Buffer.UnsafeCursor()

  private var closed: Boolean = false

  @Throws(IOException::class)
  override fun read(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }
    if (byteCount == 0L) return 0

    if (ciphered.size >= byteCount) return ciphered.read(sink, byteCount)

    refill(byteCount)
    process(cipher, source.buffer, sourceCursor, source.buffer.size, ciphered, sinkCursor)
    if (source.exhausted() && ciphered.exhausted() && cipher.getOutputSize(0) > 0) {
      throw ProtocolException("blockSize=${cipher.blockSize} unprocessed=${cipher.getOutputSize(0)}")
    }
    return ciphered.read(sink, byteCount)
  }

  private fun refill(byteCount: Long): Boolean {
    // how many ciphered bytes do we need to fulfill the request
    var needed = byteCount - ciphered.size // subtract how many we currently have
    if (cipher.blockSize > 0) {
      // pad to process block size
      val remainder = needed % cipher.blockSize
      if (remainder > 0) {
        needed += cipher.blockSize - remainder
      }
    }

    return source.request(needed)
  }

  @Throws(IOException::class)
  override fun close() {
    if (closed) return
    source.close()
    closed = true
  }

  override fun timeout(): Timeout = source.timeout()
}
