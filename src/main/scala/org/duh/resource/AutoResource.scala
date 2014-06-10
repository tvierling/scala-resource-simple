/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means. This allowance includes removal of this public domain dedication
 * and re-licensing this software under any other license.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.duh.resource

import scala.util.DynamicVariable
import scala.util.control.NonFatal

/**
 * The backing implementation of a managed resource. For a full explanation of how
 * this is commonly extended, see the [[ManagedResource]] documentation.
 *
 * @param value the resource value being manage
 * @tparam T type of the contained resource
 */
abstract class AutoResource[T](protected val value: T) extends ManagedResource[T] {
  /**
   * Allow for one class to provide both the [[AutoResource]] and the [[ManagedResource]].
   * See the documentation for [[ManagedResource]] for more information.
   *
   * @return this object
   */
  @inline final def auto: AutoResource[T] = this

  /**
   * Function implementing second-level depth of a for-comprehension.
   * Since this is only handling the resource disposal as a side effect,
   * this is identical to `map()` and simply passes through the value
   * returned by the function `f`.
   */
  @inline final def flatMap[B](f: (T) => B): B = map(f)

  /**
   * Function implementing no-`yield` code handling in a for-comprehension.
   * This simply defers to `map()` and discards any value returned.
   */
  @inline final def foreach[U](f: (T) => U): Unit = map(f)

  /**
   * The actual logic handling code wrapped by resource management, and the
   * function implementing first-level depth of a for-comprehension.
   * Since this is only handling the resource disposal as a side effect,
   * the return value is not actually a transformation of `T` in the normal
   * sense. Rather, the return value of the function `f` is returned as-is,
   * and is opaque to the code in `map()`.
   *
   * If the function `f` throws an exception, it will propagate up the stack
   * as normal, but the `close()` call will still take place.
   *
   * After executing the function `f`, but before returning its return value,
   * this calls `close()`. If an exception which qualifies as
   * [[NonFatal]] is thrown during the call to `close()`, the exception
   * handler registered with [[AutoResource.exceptionHandler]] is called
   * (it defaults to discarding the exception).
   *
   * @param f function (code block) to execute
   * @tparam B return type of the function `f`
   * @return the value returned by the function `f`
   */
  @noinline final def map[B](f: (T) => B): B = {
    try {
      f(value)
    } finally {
      try {
        close()
      } catch {
        case e if NonFatal(e) => AutoResource.exceptionHandler.value(e)
      }
    }
  }

  /**
   * Dispose of the resource represented by [[AutoResource.value]].
   * This method must be implemented by all concrete subclasses.
   */
  protected def close(): Unit
}

object AutoResource {
  /**
   * A holder for a function to handle uncaught, non-fatal `Throwable`s in the `close()` logic
   * of an [[AutoResource]]. By default, this discards the exception. The value is thread-local,
   * and should be set by calling the `withValue` method on [[DynamicVariable]] to ensure that
   * the change is reversed upon exit of code needing a special handler.
   *
   * If a new thread is spawned after this value has been altered, the new thread will gain the
   * current thread's assignment. See [[DynamicVariable]] for more information.
   */
  val exceptionHandler = new DynamicVariable[(Throwable => Unit)]({ t =>})
}
