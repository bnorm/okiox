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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okiox.coroutines.internal.await
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

class AsyncSocketTest {
  @Test fun asyncSocket() = runTest { client, server ->
    val clientSource = client.asAsyncSource().buffer()
    val clientSink = client.asAsyncSink().buffer()

    val serverSource = server.asAsyncSource().buffer()
    val serverSink = server.asAsyncSink().buffer()

    clientSink.writeUtf8("abc")
    clientSink.flush()
    assertTrue(serverSource.request(3))
    assertEquals("abc", serverSource.readUtf8(3))

    serverSink.writeUtf8("def")
    serverSink.flush()
    assertTrue(clientSource.request(3))
    assertEquals("def", clientSource.readUtf8(3))
  }

  @Test fun readUntilEof() = runTest { client, server ->
    val serverSink = client.asAsyncSink().buffer()
    val clientSource = server.asAsyncSource().buffer()

    serverSink.writeUtf8("abc")
    serverSink.close()

    assertEquals("abc", clientSource.readUtf8())
  }

  @Test fun readFailsBecauseTheSocketIsAlreadyClosed() = runTest { _, server ->
    val serverSource = server.asAsyncSource().buffer()

    server.close()

    assertFailsWith<IOException> {
      serverSource.readUtf8()
    }
  }

  @Test fun writeFailsBecauseTheSocketIsAlreadyClosed() = runTest { _, server ->
    val serverSink = server.asAsyncSink().buffer()

    server.close()

    assertFailsWith<IOException> {
      serverSink.writeUtf8("abc")
      serverSink.flush()
    }
  }

  @Test fun blockedReadFailsDueToClose() = runTest { _, server ->
    val serverSource = server.asAsyncSource().buffer()

    coroutineScope {
      launch {
        delay(500)
        server.close()
      }

      assertFailsWith<IOException> {
        serverSource.request(1)
      }
    }
  }

  @Test fun blockedWriteFailsDueToClose() = runTest { _, server ->
    val serverSink = server.asAsyncSink().buffer()

    coroutineScope {
      launch {
        delay(500)
        server.close()
      }

      assertFailsWith<IOException> {
        while (true) {
          serverSink.writeUtf8("abc")
        }
      }
    }
  }

  private fun runTest(block: suspend (client: Socket, server: Socket) -> Unit) {
    runBlocking {
      withTimeoutOrNull(25_000) {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.use {
          val serverSocket = serverSocketChannel.socket()
          serverSocket.reuseAddress = true
          serverSocketChannel.bind(InetSocketAddress(0))
          serverSocketChannel.configureBlocking(false)

          val clientChannel = SocketChannel.open()
          clientChannel.use {
            clientChannel.configureBlocking(false)
            clientChannel.connect(InetSocketAddress(serverSocket.inetAddress, serverSocket.localPort))
            clientChannel.await(SelectionKey.OP_CONNECT)
            if (!clientChannel.finishConnect()) throw IOException("connect failed")
            val client = clientChannel.socket()

            client.use {

              serverSocketChannel.await(SelectionKey.OP_ACCEPT)
              val serverChannel = serverSocketChannel.accept()
              serverChannel.configureBlocking(false)
              val server = serverChannel.socket()

              server.use {
                block(client, server)
              }
            }
          }
        }
      } ?: throw fail("test timeout")
    }
  }
}
