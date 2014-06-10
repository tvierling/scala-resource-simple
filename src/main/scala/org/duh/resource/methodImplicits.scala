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

import java.io.Closeable

import scala.language.{implicitConversions, reflectiveCalls}

/**
 * Implicit conversions based on existence of methods (duck typing).
 * These must be imported explicitly to avoid exposing conversions to [[ManagedResource]]
 * unless that behavior is specifically desired.
 */
object methodImplicits {
  /** Makes any object with a `close()` method usable as an [[AutoResource]]. */
  implicit def closeMethod[T <: {def close() : Any}](r: T): ManagedResource[T] = r match {
    // in case this type really does implement Closeable at runtime, avoid reflection
    case c: Closeable => closeableResource(c).asInstanceOf[ManagedResource[T]]

    case _ => new AutoResource[T](r) {
      override protected def close() {
        value.close()
      }
    }
  }

  /** Makes any object with a `dispose()` method usable as an [[AutoResource]]. */
  implicit def disposeMethod[T <: {def dispose() : Any}](r: T): ManagedResource[T] = new AutoResource[T](r) {
    override protected def close() {
      value.dispose()
    }
  }
}
