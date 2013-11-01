package se.kodiak.utils.yahc

import java.net.{URI, InetSocketAddress}
import se.kodiak.utils.yahc.DSL._
import se.kodiak.utils.yahc.DSL.Put
import se.kodiak.utils.yahc.DSL.Get
import se.kodiak.utils.yahc.DSL.Post
import se.kodiak.utils.yahc.DSL.Delete

object DSL {
  case class Get(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean) extends Request
  case class Post(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean, postData:String) extends Request
  case class Put(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean, putData:String) extends Request
  case class Delete(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean) extends Request
  case class Head(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean) extends Request

  sealed trait Request {
    def server:InetSocketAddress
    def path:String
    def headers:List[String]
    def secure:Boolean
  }

  /**
   * Start of with a host only.
   * @param host the host.
   * @return returns the DSL builder.
   */
  def host(host:String):DSL = {
    new DSL(host)
  }

  /**
   * Start of with a host and a port.
   * @param host the host.
   * @param port the port.
   * @return returns the DSL builder.
   */
  def host(host:String, port:Int):DSL = {
    new DSL(host, port)
  }

  /**
   * Start of with a URI object.
   * @param uri the URI object.
   * @return returns the DSL builder.
   */
  def host(uri:URI):DSL = {
    val host = uri.getHost
    val port = if (uri.getPort < 0) { 80 } else { uri.getPort }
    val dsl = new DSL(host, port)

    if (uri.getScheme.endsWith("s")) {
      dsl.secure(true)
    }

    if (!uri.getHost.charAt(0).isDigit) {
      dsl & ("Host", uri.getHost)
    }

    if (uri.getPath != null && uri.getPath != "") {
      if (uri.getPath.contains("/")) {
        uri.getPath.split("/").foreach { path =>
          if (path != "")
            dsl / path
        }
      } else {
        dsl / uri.getPath
      }
    }

    if (uri.getQuery != null && uri.getQuery != "") {
      if (uri.getQuery.contains("&")) {
        uri.getQuery.split("&").foreach { query =>
          val keyVal = query.split("=")
          dsl ? (keyVal(0), keyVal(1))
        }
      } else {
        val keyVal = uri.getQuery.split("=")
        dsl ? (keyVal(0), keyVal(1))
      }
    }

    dsl
  }

  /**
   * It makes ittebitte tiny more sense to start building with http.
   * @param host the host to connect to, later.
   * @return returns a dsl object that you can continue to build upon.
   */
  def http(host:String):DSL = new DSL(host)

  /**
   * It makes ittebitte tiny more sense to start building with http.
   * @param host the host to connect to.
   * @param port the port to connect to.
   * @return a DSL to continue build your request on.
   */
  def http(host:String, port:Int):DSL = new DSL(host, port)

  /**
   * It makes ittebitte tiny more sense to start building with http.
   * @param host the secure host to connecto to.
   * @return a DSL to continue build your request on.
   */
  def https(host:String):DSL = new DSL(host).secure(true)

  /**
   * It makes ittebitte tiny more sense to start building with http.
   * @param host the secure host to connect to.
   * @param port the secure port to connect to.
   * @return a DLS to continue build your request on.
   */
  def https(host:String, port:Int):DSL = new DSL(host, port).secure(true)

  implicit def strToDSL(uri:String):DSL = {
    val url = new URI(uri)
    host(url)
  }
}

/**
 * A DSL builder to generate HTTP requests.
 * @param host the host of this request.
 * @param port the port of this request, defaults to 80.
 */
class DSL(val host:String, val port:Int = 80) {
  private var queryParams = List[(String, String)]()
  private var postParams = List[(String, String)]()
  private var headerParams = List[(String, String)]()
  private var paths = List[String]()
  private var https = false

  /**
   * Add a queryParam tuple.
   * @param queryParam the tuple.
   * @return returns this DSL instance.
   */
  def ?(queryParam:(String, String)):DSL = {
    queryParams = queryParams :+ queryParam
    this
  }

  /**
   * Return the current list of query param tuples.
   * @return the list of tuples.
   */
  def getQueryParams:List[(String, String)] = queryParams

  /**
   * Add a post/put param tuple.
   * @param postParam the tuple.
   * @return returns this DSL instance.
   */
  def +(postParam:(String, String)):DSL = {
    postParams = postParams :+ postParam
    this
  }

  /**
   * Return a list of post/put param tuples.
   * @return the list of tuples.
   */
  def getPostParams:List[(String, String)] = postParams

  /**
   * Set secure (true=https, false=http) default value is false.
   * @param secure go secure or not.
   */
  def secure(secure:Boolean):DSL = {
    https = secure
    this
  }

  /**
   * Returns weather this request is set to secure or not.
   * @return TorF
   */
  def secure:Boolean = https

  /**
   * Add a header tuple.
   * @param headerParam the tuple.
   * @return returns this DSL instance.
   */
  def &(headerParam:(String,String)):DSL = {
    headerParams = headerParams :+ headerParam
    this
  }

  /**
   * Returns the current list of header tuples.
   * @return the list of tuples.
   */
  def getHeaderParams = headerParams

  /**
   * Append some bit of path to the total path.
   * @param path the bit to append.
   * @return returns this DSL instance.
   */
  def /(path:String):DSL = {
    this.paths = this.paths :+ path
    this
  }

  private def buildPath:String = {
    val path = "/"+paths.mkString("/")
    val query = getQueryParams.map { f =>
      f._1+"="+f._2
    }

    if (query.size > 0) {
      path + "?" + query.mkString("&")
    } else {
      path
    }
  }

  private def buildHeaders(headers:List[(String, String)]):List[String] = {
    var list:List[(String, String)] = headers
    if (list == null) {
      list = headerParams
    }

    list.map { h =>
      h._1+":"+h._2
    }
  }

  /**
   * Generate a GET request from this builder.
   * @return a GET reuest.
   */
  def get:Get = {
    new Get(new InetSocketAddress(host, port), buildPath, buildHeaders(null), secure)
  }

  /**
   * Returns a POST request, with form encoded data.
   * @return the POST request.
   */
  def post:Post = {
    val post = postParams.map { p =>
      p._1+"="+p._2
    }
    val headers = headerParams :+ ("Content-Length", post.mkString("&").length.toString) :+ ("Content-Encoding", "application/x-www-form-urlencoded")
    new Post(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, post.mkString("&"))
  }

  /**
   * Returns a POST request with post data set to postData. Dont forget to set the content-encoding header.
   * @param postData the "raw" data to post.
   * @return the POST request.
   */
  def post(postData:String):Post = {
    // TODO funny how it reads string and returns array[byte]..
    val headers = headerParams :+ ("Content-Length", postData.length.toString)
    new Post(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, postData)
  }

  /**
   * Generate a PUT request from this builder with form encoded data.
   * @return the PUT request
   */
  def put:Put = {
    val put = postParams.map { p =>
      p._1+"="+p._2
    }
    val headers = headerParams :+ ("Content-Length", put.mkString("&").length.toString) :+ ("Content-Encoding", "application/x-www-form-urlencoded")
    new Put(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, put.mkString("&"))
  }

  /**
   * Generate a PUT request from the data from parameter putData. Dont forget to set the content-encoding header.
   * @param putData the data to PUT.
   * @return the PUT request.
   */
  def put(putData:String):Put = {
    // TODO funny how it reads string and returns array[byte]..
    val headers = headerParams :+ ("Content-Length", putData.length.toString)
    new Put(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, putData)
  }

  /**
   * Generate a DELETE request from this builder.
   * @return the DELETE request.
   */
  def delete:Delete = {
    new Delete(new InetSocketAddress(host, port), buildPath, buildHeaders(null), secure)
  }

  /**
   * Generate a HEAD request from this builder.
   * @return the HEAD request.
   */
  def head:Head = {
    new Head(new InetSocketAddress(host, port), buildPath, buildHeaders(null), secure)
  }
}