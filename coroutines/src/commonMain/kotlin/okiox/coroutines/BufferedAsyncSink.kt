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

package okiox.coroutines

import okio.Buffer
import okio.ByteString
import okio.IOException
import okiox.coroutines.internal.Throws

/**
 * Returns a new source that buffers reads from `source`. The returned source will perform bulk
 * reads into its in-memory buffer. Use this wherever you read a source to get an ergonomic and
 * efficient access to data.
 */
fun AsyncSource.buffer(): BufferedAsyncSource = RealBufferedAsyncSource(this)

/**
 * A sink that keeps a buffer internally so that callers can do small writes without a performance
 * penalty.
 */
interface BufferedAsyncSink : AsyncSink {
  /** This sink's internal buffer. */
  val buffer: Buffer

  @Throws(IOException::class)
  suspend fun write(byteString: ByteString): BufferedAsyncSink

  /** Like OutputStream.write, this writes `byteCount` bytes of `source`, starting at `offset`. */
  @Throws(IOException::class)
  suspend fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size): BufferedAsyncSink

  /**
   * Removes all bytes from `source` and appends them to this sink. Returns the number of bytes read
   * which will be 0 if `source` is exhausted.
   */
  @Throws(IOException::class)
  suspend fun writeAll(source: AsyncSource): Long

  /** Removes `byteCount` bytes from `source` and appends them to this sink. */
  @Throws(IOException::class)
  suspend fun write(source: AsyncSource, byteCount: Long): BufferedAsyncSink

  /**
   * Encodes the characters at `beginIndex` up to `endIndex` from `string` in UTF-8 and writes it to
   * this sink.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeUtf8("I'm a hacker!\n", 6, 12);
   * buffer.writeByte(' ');
   * buffer.writeUtf8("That's what I said: you're a nerd.\n", 29, 33);
   * buffer.writeByte(' ');
   * buffer.writeUtf8("I prefer to be called a hacker!\n", 24, 31);
   *
   * assertEquals("hacker nerd hacker!", buffer.readUtf8());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): BufferedAsyncSink

  /** Encodes `codePoint` in UTF-8 and writes it to this sink. */
  @Throws(IOException::class)
  suspend fun writeUtf8CodePoint(codePoint: Int): BufferedAsyncSink

  /** Writes a byte to this sink. */
  @Throws(IOException::class)
  suspend fun writeByte(b: Int): BufferedAsyncSink

  /**
   * Writes a big-endian short to this sink using two bytes.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeShort(32767);
   * buffer.writeShort(15);
   *
   * assertEquals(4, buffer.size());
   * assertEquals((byte) 0x7f, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x0f, buffer.readByte());
   * assertEquals(0, buffer.size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeShort(s: Int): BufferedAsyncSink

  /**
   * Writes a little-endian short to this sink using two bytes.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeShortLe(32767);
   * buffer.writeShortLe(15);
   *
   * assertEquals(4, buffer.size());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0x7f, buffer.readByte());
   * assertEquals((byte) 0x0f, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals(0, buffer.size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeShortLe(s: Int): BufferedAsyncSink

  /**
   * Writes a big-endian int to this sink using four bytes.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeInt(2147483647);
   * buffer.writeInt(15);
   *
   * assertEquals(8, buffer.size());
   * assertEquals((byte) 0x7f, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x0f, buffer.readByte());
   * assertEquals(0, buffer.size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeInt(i: Int): BufferedAsyncSink

  /**
   * Writes a little-endian int to this sink using four bytes.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeIntLe(2147483647);
   * buffer.writeIntLe(15);
   *
   * assertEquals(8, buffer.size());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0x7f, buffer.readByte());
   * assertEquals((byte) 0x0f, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals(0, buffer.size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeIntLe(i: Int): BufferedAsyncSink

  /**
   * Writes a big-endian long to this sink using eight bytes.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeLong(9223372036854775807L);
   * buffer.writeLong(15);
   *
   * assertEquals(16, buffer.size());
   * assertEquals((byte) 0x7f, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x0f, buffer.readByte());
   * assertEquals(0, buffer.size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeLong(v: Long): BufferedAsyncSink

  /**
   * Writes a little-endian long to this sink using eight bytes.
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeLongLe(9223372036854775807L);
   * buffer.writeLongLe(15);
   *
   * assertEquals(16, buffer.size());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0xff, buffer.readByte());
   * assertEquals((byte) 0x7f, buffer.readByte());
   * assertEquals((byte) 0x0f, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals((byte) 0x00, buffer.readByte());
   * assertEquals(0, buffer.size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeLongLe(v: Long): BufferedAsyncSink

  /**
   * Writes a long to this sink in signed decimal form (i.e., as a string in base 10).
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeDecimalLong(8675309L);
   * buffer.writeByte(' ');
   * buffer.writeDecimalLong(-123L);
   * buffer.writeByte(' ');
   * buffer.writeDecimalLong(1L);
   *
   * assertEquals("8675309 -123 1", buffer.readUtf8());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeDecimalLong(v: Long): BufferedAsyncSink

  /**
   * Writes a long to this sink in hexadecimal form (i.e., as a string in base 16).
   * ```
   * Buffer buffer = new Buffer();
   * buffer.writeHexadecimalUnsignedLong(65535L);
   * buffer.writeByte(' ');
   * buffer.writeHexadecimalUnsignedLong(0xcafebabeL);
   * buffer.writeByte(' ');
   * buffer.writeHexadecimalUnsignedLong(0x10L);
   *
   * assertEquals("ffff cafebabe 10", buffer.readUtf8());
   * ```
   */
  @Throws(IOException::class)
  suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedAsyncSink

  /**
   * Writes all buffered data to the underlying sink, if one exists. Then that sink is recursively
   * flushed which pushes data as far as possible towards its ultimate destination. Typically that
   * destination is a network socket or file.
   * ```
   * BufferedAsyncSink b0 = new Buffer();
   * BufferedAsyncSink b1 = Okio.buffer(b0);
   * BufferedAsyncSink b2 = Okio.buffer(b1);
   *
   * b2.writeUtf8("hello");
   * assertEquals(5, b2.buffer().size());
   * assertEquals(0, b1.buffer().size());
   * assertEquals(0, b0.buffer().size());
   *
   * b2.flush();
   * assertEquals(0, b2.buffer().size());
   * assertEquals(0, b1.buffer().size());
   * assertEquals(5, b0.buffer().size());
   * ```
   */
  @Throws(IOException::class)
  override suspend fun flush()

  /**
   * Writes all buffered data to the underlying sink, if one exists. Like [flush], but weaker. Call
   * this before this buffered sink goes out of scope so that its data can reach its destination.
   * ```
   * BufferedAsyncSink b0 = new Buffer();
   * BufferedAsyncSink b1 = Okio.buffer(b0);
   * BufferedAsyncSink b2 = Okio.buffer(b1);
   *
   * b2.writeUtf8("hello");
   * assertEquals(5, b2.buffer().size());
   * assertEquals(0, b1.buffer().size());
   * assertEquals(0, b0.buffer().size());
   *
   * b2.emit();
   * assertEquals(0, b2.buffer().size());
   * assertEquals(5, b1.buffer().size());
   * assertEquals(0, b0.buffer().size());
   *
   * b1.emit();
   * assertEquals(0, b2.buffer().size());
   * assertEquals(0, b1.buffer().size());
   * assertEquals(5, b0.buffer().size());
   * ```
   */
  @Throws(IOException::class)
  suspend fun emit(): BufferedAsyncSink

  /**
   * Writes complete segments to the underlying sink, if one exists. Like [flush], but weaker. Use
   * this to limit the memory held in the buffer to a single segment. Typically application code
   * will not need to call this: it is only necessary when application code writes directly to this
   * [sink's buffer][buffer].
   * ```
   * BufferedAsyncSink b0 = new Buffer();
   * BufferedAsyncSink b1 = Okio.buffer(b0);
   * BufferedAsyncSink b2 = Okio.buffer(b1);
   *
   * b2.buffer().write(new byte[20_000]);
   * assertEquals(20_000, b2.buffer().size());
   * assertEquals(     0, b1.buffer().size());
   * assertEquals(     0, b0.buffer().size());
   *
   * b2.emitCompleteSegments();
   * assertEquals( 3_616, b2.buffer().size());
   * assertEquals(     0, b1.buffer().size());
   * assertEquals(16_384, b0.buffer().size()); // This example assumes 8192 byte segments.
   * ```
   */
  @Throws(IOException::class)
  suspend fun emitCompleteSegments(): BufferedAsyncSink
}
