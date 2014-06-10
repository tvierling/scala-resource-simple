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

package org.duh

import java.io._

import scala.language.implicitConversions

package object resource {
  /** The most common use case: a resource that implements [[Closeable]]. */
  implicit def closeableResource[T <: Closeable](r: T): ManagedResource[T] = new AutoResource[T](r) {
    override protected def close() {
      value.close()
    }
  }

  /** Separate conversion to isolate Java 7 [[AutoCloseable]] support for classes that are not [[Closeable]]. */
  implicit def autoCloseableResource[T <: AutoCloseable](r: T): ManagedResource[T] = new AutoResource[T](r) {
    override protected def close() {
      value.close()
    }
  }
}
