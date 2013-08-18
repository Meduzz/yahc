package se.kodiak.utils.yahc

import akka.util.{ByteIterator, ByteString}
import se.kodiak.utils.yahc.DSL._
import scala.annotation.tailrec

/**
 * Converts requests to ByteStrings to be written
 * and ByteStrings to Responses to be sent back.
 */
object HttpUtil {
  val LF = "\r\n"

  /**
   * Handles all supported requests (DSL._ verbs)
   * @param req the request.
   * @return returns a bytestring of the request.
   */
  def apply(req:Request):ByteString = {
    val builder = ByteString.newBuilder

    builder.putBytes(startOut(req).getBytes("UTF-8"))
    builder.putBytes((headers(req)+LF).getBytes("UTF-8"))
    builder.putBytes(LF.getBytes("UTF-8"))
    req match {
      case p:Post => builder.putBytes(p.postData.getBytes("UTF-8"))
      case p:Put => builder.putBytes(p.putData.getBytes("UTF-8"))
      case _ => // ignore.
    }

    builder.result()
  }

  /**
   * Takes the raw response from the server and coverts it to a Response.
   * @param res a bytestring, ie the bytes sent by the server.
   * @return returns a Request.
   */
  def apply(res:ByteString):Response = {
    val it = res.iterator
    val headerBody = StringBuilder.newBuilder
    val bodyPos = findBody(it, headerBody, 1)

    var startAndHeaders = ""
    var body = Array[Byte]()

    if (bodyPos+5 > res.size) {
      startAndHeaders = headerBody.result()
    } else {
      startAndHeaders = headerBody.result().substring(0, bodyPos)
      body = new Array[Byte](res.size-bodyPos-8)
      val newIt = res.iterator
      newIt.drop(bodyPos+4)
      newIt.getBytes(body)
    }

    val headerLines = startAndHeaders.split(LF)
    val Array(http, code, msg) = headerLines.head.split(" ")
    val headers = headerLines.tail.map { l =>
      val header = l.split(":")
      (header(0) -> header(1))
    }.toMap
    new Response(code.toInt, msg, headers, body)
  }

  private def startOut(req:Request):String = {
    req match {
      case Get(host, path, _, _) => "GET "+path+" HTTP/1.1"+LF
      case Post(host, path, _, _, _) => "POST "+path+" HTTP/1.1"+LF
      case Put(host, path, _, _, _) => "PUT "+path+" HTTP/1.1"+LF
      case Delete(host, path, _, _) => "DELETE "+path+" HTTP/1.1"+LF
      case Head(host, path, _, _) => "HEAD "+path+" HTTP/1.1"+LF
    }
  }

  private def headers(req:Request):String = {
    req.headers.mkString(LF)
  }

  @tailrec
  private def findBody(it:ByteIterator, body:StringBuilder, takes:Int):Int = {
    if (it.len < 128) {
      val buffer = new Array[Byte](it.len)
      it.getBytes(buffer)
      body.append(new String(buffer, "UTF-8"))
      return body.result().indexOf(LF+LF)
    } else {
      val buffer = new Array[Byte](128)
      it.getBytes(buffer)
      body.append(new String(buffer, "UTF-8"))
    }

    if (!body.result().contains(LF+LF) && ((takes+1)*128) < it.size) {
      findBody(it, body, takes+1)
    } else {
      body.result().indexOf(LF+LF)
    }
  }
}
