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

@file:JvmName("-FilesJvm")
@file:Suppress("BlockingMethodInNonBlockingContext")

package okiox.coroutines.internal

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend inline fun SelectableChannel.await(ops: Int) {
  selector.waitForSelection(this, ops)
}

internal val selector: SelectorThread by lazy {
  val result = SelectorThread()
  result.start()
  result
}

internal class SelectorThread : Thread("okiox selector") {
  init {
    isDaemon = true
  }

  private val selector = Selector.open()
  private val keys = ConcurrentLinkedQueue<SelectionKey>()

  suspend fun waitForSelection(channel: SelectableChannel, ops: Int) {
    suspendCancellableCoroutine<SelectionKey> { cont ->
      val key = channel.register(selector, ops, cont)
      check(key.attachment() === cont) { "already registered" }

      cont.invokeOnCancellation {
        key.cancel()
        selector.wakeup()
      }

      keys.add(key)
      selector.wakeup()
    }
  }

  override fun run() {
    while (true) {
      selector.select()
      selector.selectedKeys().clear()

      val iter = keys.iterator()
      while (iter.hasNext()) {
        val key = iter.next()

        if (!key.isValid) {
          @Suppress("UNCHECKED_CAST")
          val cont = key.attach(null) as CancellableContinuation<SelectionKey>
          if (!cont.isCompleted) cont.resumeWithException(IOException("closed"))
          iter.remove()
        } else if (key.readyOps() > 0) {
          @Suppress("UNCHECKED_CAST")
          val cont = key.attach(null) as CancellableContinuation<SelectionKey>
          cont.resume(key)
          iter.remove()
        }
      }
    }
  }
}
