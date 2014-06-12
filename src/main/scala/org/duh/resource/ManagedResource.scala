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

/**
 * An automatically managed resource which is closed before the code block handling it returns.
 * This is intended to be used in a for-comprehension such as:
 *
 * {{{
 *   for (x <- new FooResource().auto) {
 *     // ...code using x...
 *   }
 * }}}
 *
 * which will call `x.close()` (or an equivalent method via an [[AutoResource]] subclass)
 * when the code block goes out of scope. It is also legal to use the monad methods directly,
 * such as the following equivalent code to the above:
 *
 * {{{
 *   new FooResource().auto.foreach { x =>
 *     // ...code using x...
 *   }
 * }}}
 *
 * though the for-comprehension is usually more intuitive, and provides a more concise syntax
 * for situations where multiple resources are being managed at the same time. Direct use of
 * the methods `foreach` and `map` work well as pass-through operations, where a function
 * object instance is already available.
 *
 * Generally this class is implemented by an implicit conversion which has `ManagedResource[T]`
 * in its return type signature rather than `AutoResource[T]`, while the actual implementation
 * class actually is a subclass of [[AutoResource]]. This avoids an extra layer of allocation,
 * while hiding the `foreach`, `flatMap`, and `map` methods which may not be desirable on
 * arbitrary types:
 *
 * {{{
 *   implicit def myResource[T <: MyType](r: T): ManagedResource[T] = new AutoResource[T](r) {
 *     override protected def close(value: T) {
 *       // use arg "value", not "r", below to avoid an extra class field for "r"
 *       value.myCloseMethod()
 *     }
 *   }
 * }}}
 *
 * However, in some situations it may be more useful to have the returned [[AutoResource]] be
 * defined by something else, such as a common trait or superclass which is not itself exposed
 * to implicit conversion to [[ManagedResource]]. In that case, a value class implementation
 * can be used to avoid the overhead of an extra layer of objects:
 *
 * {{{
 *   implicit class MyResource[T <: MyType](private val r: T) extends AnyVal with ManagedResource[T] {
 *     def auto: AutoResource[T] = new MyAutoResource[T](r)
 *   }
 * }}}
 *
 * @tparam T type of the contained resource
 */
trait ManagedResource[T] extends Any {
  def auto: AutoResource[T]
}
