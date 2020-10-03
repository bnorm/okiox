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
import java.io.IOException
import javax.crypto.Cipher

/**
 * Drain `byteCount` bytes from `source`, run them through the cipher, and write all
 * processed output into `sink`.
 */
internal fun process(
  cipher: Cipher,
  source: Buffer,
  sourceCursor: Buffer.UnsafeCursor,
  byteCount: Long,
  sink: Buffer,
  sinkCursor: Buffer.UnsafeCursor
) {
  require(byteCount >= 0) { "byteCount < 0: $byteCount" }
  require(byteCount <= source.size) { "size=" + source.size + " byteCount=" + byteCount }

  source.readUnsafe(sourceCursor)
  sink.readAndWriteUnsafe(sinkCursor)
  try {
    sourceCursor.seek(0)

    var remaining = byteCount
    while (remaining > 0) {
      val inputSize = minOf((sourceCursor.end - sourceCursor.start).toLong(), remaining).toInt()
      var outputSize = cipher.getOutputSize(inputSize)
      if (cipher.blockSize > 0) {
        // Block ciphers output data in BlockSize chunks
        outputSize -= outputSize % cipher.blockSize
      }
      if (outputSize > 8192) {
        throw AssertionError(
          String.format(
            "existing=%d blockSize=%d inputSize=%d outputSize=%s",
            cipher.getOutputSize(0), cipher.blockSize, inputSize, outputSize
          )
        )
      }

      if (outputSize == 0) {
        // No output, but add data to the cipher
        cipher.update(
          sourceCursor.data,
          sourceCursor.start,
          inputSize
        )
      } else {
        val oldSize = sink.size
        sinkCursor.expandBuffer(outputSize)
        val update = cipher.update(
          sourceCursor.data,
          sourceCursor.start,
          inputSize,
          sinkCursor.data,
          sinkCursor.start
        )
        sinkCursor.resizeBuffer(oldSize + update)
      }

      sourceCursor.seek(sourceCursor.offset + inputSize)
      remaining -= inputSize.toLong()
    }
  } finally {
    sourceCursor.close()
    sinkCursor.close()
  }

  source.skip(byteCount)
}
