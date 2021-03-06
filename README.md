scala-resource-simple
=====================

[![Join the chat at https://gitter.im/tvierling/scala-resource-simple](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/tvierling/scala-resource-simple?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Codacy Badge](https://www.codacy.com/project/badge/6077971591d440d6a4041b918b012da4)](https://www.codacy.com/app/tv/scala-resource-simple_2) [![Build Status](https://drone.io/github.com/tvierling/scala-resource-simple/status.png)](https://drone.io/github.com/tvierling/scala-resource-simple/latest)

**Lightweight automatic resource management for Scala**

This small module for Scala projects provides an automatic resource management method inspired by the support added to Java 7 in its ["try-with-resources"][javaarm] construct.

The library is similar in concept to the [scala-arm][] project, but is intended to be far more lightweight; this implementation is meant to provide cleanup safety for resources only, and nothing more. With the exception of two optional structural type conversions, `scala-resource-simple` doesn't use reflection, `Manifest`s, or any other high-overhead code.

[javaarm]: http://www.oracle.com/technetwork/articles/java/trywithresources-401775.html
[scala-arm]: https://github.com/jsuereth/scala-arm


Importing into your project
---------------------------

In SBT (will automatically grab the build for the appropriate Scala version):

    libraryDependencies += "org.duh" %% "scala-resource-simple" % "0.3"

*or* to grab builds for all Scala versions:

    libraryDependencies += "org.duh" %% "scala-resource-simple_2.10" % "0.3"
    libraryDependencies += "org.duh" %% "scala-resource-simple_2.11" % "0.3"

In Maven:

    <dependency>
       <groupId>org.duh</groupId>
       <artifactId>scala-resource-simple_2.11</artifactId>
       <version>0.3</version>
    </dependency>

In Ivy:

    <dependency org="org.duh" name="scala-resource-simple_2.11" rev="0.3"/>

In Gradle:

    compile 'org.duh:scala-resource-simple_2.11:0.3'

If you are using Scala 2.10, replace the `2.11` instances above with `2.10` in the Maven, Ivy, or Gradle examples.


Manual Build
------------

Building from source involves using `sbt` 0.13 or later.

Run the following to build for Scala 2.11:

    sbt package

To generate builds for both Scala 2.10 and 2.11, run:

    sbt "+ package"

If `sbt` is not installed on your system, you can use a bundled sbt launcher from this source tree. Substitute `sh project/sbt.sh` for `sbt` in the commands above.

Or, if you have Apache Ant installed, you can run `ant build` to do the equivalent of `sbt "+ package"` above, which automatically uses the bundled sbt launcher.


Basic Usage
-----------

The latest snapshot scaladoc is [available here](https://tvierling.github.io/scala-resource-simple/latest/api/).

By importing the main package, implicit conversions for `java.io.Closeable` and `java.lang.AutoCloseable` are automatically in scope. Use of any resource implementing these interfaces is as simple as adding `.auto' to the end of the resource in a for-comprehension, such as:

    import org.duh.resource._
    
    def helloFile(filename: String) {
      for (a <- new FileWriter(filename).auto) {
        // type of "a" is FileWriter inside this block
        a.write("hello world\n")
      }
    }

When the code block exits, whether normally or by an exception, `a.close()` is automatically called. Multiple resources can be managed in the same `for` expression, and will be closed in reverse order on exit of the block:

    def copyFirstLine(filename: String, infile: String) {
      for (a <- new FileWriter(filename).auto;
           b <- new BufferedReader(new FileReader(infile)).auto) {
        // type of "a" is FileWriter; type of "b" is BufferedReader
        a.write(b.readLine())
      }
    }

Note that the operation used in the `for` expression is a constructor. Normally, this should be construction of a `new` object, or call of a method that expects the caller to close the returned object, for instance:

    def writeHTTPBody(conn: java.net.URLConnection, data: Array[Byte]) {
      for (a <- conn.getOutputStream.auto) {
        a.write(data)
      }
    }

Here, the `OutputStream` from `URLConnection` is automatically closed as the method returns.


Mixing for-comprehensions
-------------------------

It's also possible to mix resource management and other for-comprehensions:

    def writeManyFiles(filenamePrefix: String, numfiles: Int) {
      for (i <- 1 to numfiles; out <- new FileWriter(filenamePrefix + i).auto) {
        out.write("file number " + i)
      }
    }

Each file is automatically closed before `i` is advanced to the next index in the `Range`.


Returning a value
-----------------

Resource management operations allow for values to be passed out of the block via the `yield` operator:

    def readFirstLine(infile: String): String = {
      for (in <- new BufferedReader(new FileReader(infile)).auto) {
        yield in.readLine()
      }
    }

    def readFirstLinePrefixed(infile: String): String = {
      val line = for (in <- new BufferedReader(new FileReader(infile)).auto) {
        yield in.readLine()
      }
      
      infile + " : " + line
    }


What about other types with a `close()` method?
-----------------------------------------------

As of this writing, besides `Closeable` and `AutoCloseable`, two other conversions exist which use Scala's structural types to make any object with a `close()` or `dispose()` method manageable with the `.auto` conversion. They are not imported automatically by `import org.duh.resource._`; they must be imported separately:

    import org.duh.resource.methodImplicits._

This is particularly useful prior to Java 7 (which peppered the standard library with `Closeable` in many places where it was previously missing), but also allows for many other types which do not have specific implicit conversions available.


What about other type conversions?
----------------------------------

It's simple to implement a new conversion which provides the `.auto` method. See the [documentation for `ManagedResource`](https://tvierling.github.io/scala-resource-simple/latest/api/org/duh/resource/ManagedResource.html) for a full definition. The short version is this:

    // this type wants you to call squash() to release its resources
    trait MySquashable {
      def squash(): Unit
    }
    
    implicit def mySquashableResource[T <: MySquashable](r: T): ManagedResource[T] =
      new AutoResource[T](r) {
        // use arg "value", not "r", below to avoid an extra class field for "r"
        override protected def close(value: T) { value.squash() }
      }

Note that the return type of the method is explicitly declared to be `ManagedResource[T]`, *not* `AutoResource[T]`. This allows exposure of the `auto` method *without* exposing `foreach`, `flatMap`, and `map` as implicit conversion methods on the type `MySquashable` (which could lead to surprising results if used directly).


What about exceptions thrown by `close()`?
------------------------------------------

There is a scoped function holder in [`AutoResource.exceptionHandler`](https://tvierling.github.io/scala-resource-simple/latest/api/org/duh/resource/AutoResource$.html). This is an instance of [`DynamicVariable`](http://www.scala-lang.org/api/current/scala/util/DynamicVariable.html), which can be set for a given code block and automatically reset afterwards:

    def readFirstLine(infile: String): String = {
      AutoResource.exceptionHandler.withValue(e => e.printStackTrace()) {
        for (in <- new BufferedReader(new FileReader(infile))) {
          yield in.readLine()
        }
      }
    }

Above, if `readLine()` throws an exception, it bubbles up the stack and is thrown back to the caller of `readFirstLine()`. However, if an exception is thrown by `BufferedReader.close()`, it is caught and its stack trace is printed.

By default, `exceptionHandler` simply discards the exception thrown during `close()`. See its documentation for more info.


License
-------

scala-resource-simple is made available under the Unlicense (modified to add express permission to remove the Unlicense text itself), which releases this code into the public domain for any purpose. The license text with each source file is:

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

(Why public domain? This library is so small that it makes no sense to take pains to license it any other way.)


Contacting the author
---------------------

Besides adding issues / feature requests on GitHub, you can also reach the author, Todd Vierling, at: tv@duh.org
