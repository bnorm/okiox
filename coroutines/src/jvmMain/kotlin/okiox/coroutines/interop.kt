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

@file:JvmName("-JvmInterop")

package okiox.coroutines

import kotlinx.coroutines.Dispatchers
import okio.Sink
import okio.Source
import okiox.coroutines.internal.ForwardingAsyncSink
import okiox.coroutines.internal.ForwardingAsyncSource
import okiox.coroutines.internal.ForwardingSink
import okiox.coroutines.internal.ForwardingSource
import kotlin.coroutines.CoroutineContext

fun Sink.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSink {
  if (this is ForwardingSink) return this.delegate
  return ForwardingAsyncSink(this, context)
}

fun AsyncSink.toBlocking(): Sink {
  if (this is ForwardingAsyncSink) return this.delegate
  return ForwardingSink(this)
}

fun Source.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSource {
  if (this is ForwardingSource) return this.delegate
  return ForwardingAsyncSource(this, context)
}

fun AsyncSource.toBlocking(): Source {
  if (this is ForwardingAsyncSource) return this.delegate
  return ForwardingSource(this)
}
