scala-resource-simple
=====================

**Lightweight automatic resource management for Scala**

This small module for Scala projects provides an automatic resource management method inspired by the support added to Java 7 in its ["try-with-resources"][javaarm] construct.

The library is similar in concept to the [scala-arm][] project, but is intended to be far more lightweight; this implementation is meant to provide cleanup safety for resources only, and nothing more. With the exception of two optional structural type conversions, `scala-resource-simple` doesn't use reflection, `Manifest`s, or any other high-overhead code.


Importing into your project
---------------------------

As of this writing, scala-resource-simple is not published to a Maven repository yet. It is in an alpha-quality state, though the fundamental operation (managing a resource through a for-comprehension) works. I intend to publish it after some thorough unit tests are implemented.

To build a jar file, simply run `sbt package`. The output jar can be found in `target/scala-2.11/scala-resource-simple_2.11-VERSION.jar`. By default, this compiles with Scala 2.11, but it will build with 2.10 as well. To do that, run sbt interactively, then enter:

    > ++ "2.10.4"
    > package

and you will have a `target/scala-2.10/scala-resource-simple_2.10-VERSION.jar`. Or, to build for both Scala 2.10 and 2.11 at the same time, run sbt interactively and enter:

    > + package


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
        // use "value", not "r", below to avoid an extra hidden field
        override protected def close() { value.squash() }
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


[javaarm]: http://www.oracle.com/technetwork/articles/java/trywithresources-401775.html
[scala-arm]: https://github.com/jsuereth/scala-arm
