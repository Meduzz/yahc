package se.kodiak.utils.yahc

import java.net.{URI, InetSocketAddress}
import se.kodiak.utils.yahc.DSL._
import se.kodiak.utils.yahc.DSL.Put
import se.kodiak.utils.yahc.DSL.Get
import se.kodiak.utils.yahc.DSL.Post
import se.kodiak.utils.yahc.DSL.Delete

object DSL {
  case class Get(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean)
  case class Post(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean, postData:String)
  case class Put(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean, putData:String)
  case class Delete(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean)
  case class Headers(server:InetSocketAddress, path:String, headers:List[String] = List(), secure:Boolean)

  def host(url:String):DSL = {
    new DSL(url)
  }

  def host(host:String, port:Int):DSL = {
    new DSL(host, port)
  }

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

  implicit def strToDSL(uri:String):DSL = {
    val url = new URI(uri)
    host(url)
  }
}

class DSL(val host:String, val port:Int = 80) {
  private var queryParams = List[(String, String)]()
  private var postParams = List[(String, String)]()
  private var headerParams = List[(String, String)]()
  private var paths = List[String]()
  private var https = false

  def ?(queryParam:(String, String)):DSL = {
    println("Q: "+queryParam._1 + "&" + queryParam._2) // TODO remove
    queryParams = queryParams :+ queryParam
    this
  }

  def getQueryParams:List[(String, String)] = queryParams

  def +(postParam:(String, String)):DSL = {
    println("P: "+postParam._1 + "&" + postParam._2) // TODO remove
    postParams = postParams :+ postParam
    this
  }

  def getPostParams:List[(String, String)] = postParams

  def secure(secure:Boolean) = {
    https = secure
  }

  def secure:Boolean = https

  def &(headerParam:(String,String)):DSL = {
    println("H: "+headerParam._1 + "&" + headerParam._2) // TODO remove
    headerParams = headerParams :+ headerParam
    this
  }

  def getHeaderParams = headerParams

  def /(path:String):DSL = {
    println("A: "+path) // TODO remove
    this.paths = this.paths :+ path
    this
  }

  private def buildPath:String = {
    val path = "/"+paths.mkString("/")
    val query = getQueryParams.map { f =>
      f._1+"="+f._2
    }
    path + "?"+query.mkString("&")
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

  def get:Get = {
    new Get(new InetSocketAddress(host, port), buildPath, buildHeaders(null), secure)
  }

  def post:Post = {
    val post = postParams.map { p =>
      p._1+"="+p._2
    }
    val headers = headerParams :+ ("Content-Length", post.mkString("&").length.toString) :+ ("Content-Encoding", "application/x-www-form-urlencoded")
    new Post(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, post.mkString("&"))
  }

  def post(postData:String):Post = {
    val headers = headerParams :+ ("Content-Length", postData.length.toString)
    new Post(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, postData)
  }

  def put:Put = {
    val put = postParams.map { p =>
      p._1+"="+p._2
    }
    val headers = headerParams :+ ("Content-Length", put.mkString("&").length.toString) :+ ("Content-Encoding", "application/x-www-form-urlencoded")
    new Put(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, put.mkString("&"))
  }

  def put(putData:String):Put = {
    val headers = headerParams :+ ("Content-Length", putData.length.toString)
    new Put(new InetSocketAddress(host, port), buildPath, buildHeaders(headers), secure, putData)
  }

  def delete:Delete = {
    new Delete(new InetSocketAddress(host, port), buildPath, buildHeaders(null), secure)
  }

  def headers:Headers = {
    new Headers(new InetSocketAddress(host, port), buildPath, buildHeaders(null), secure)
  }
}