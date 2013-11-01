package se.kodiak.utils.yahc

import akka.util.{ByteStringBuilder, ByteIterator, ByteString}
import se.kodiak.utils.yahc.DSL._
import scala.annotation.tailrec

/**
 * Converts requests to ByteStrings to be written
 * and ByteStrings to Responses to be sent back.
 */
object HttpUtil {
  val CR = 13.toChar
  val LF = 10.toChar
  val CRLF = s"${CR}${LF}"
  val CRLF2 = CRLF+CRLF

  /**
   * Handles all supported requests (DSL._ verbs)
   * @param req the request.
   * @return returns a bytestring of the request.
   */
  def apply(req:Request):ByteString = {
    // TODO return a class/actor instead of an object
    val builder = ByteString.newBuilder

    builder.putBytes(startOut(req).getBytes("UTF-8"))
    builder.putBytes((headers(req)+CRLF).getBytes("UTF-8"))
    builder.putBytes(CRLF.getBytes("UTF-8"))
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
  def apply(res:ByteString):Option[ResponseDTO] = {
    // TODO return a class/actor instead of an object!

    val it = res.iterator
    val bodyStart = findNeedleInBuffer(res, CRLF2, 0)
    val bodyEnd = findNeedleInBuffer(res, CRLF2, bodyStart + 4)
    val headerBuffer = new Array[Byte](bodyStart)
    var bodyBuffer = Array[Byte]()
    var totalLen = headerBuffer.length + 4
    it.getBytes(headerBuffer)
    it.drop(4)

    val headerLines = new String(headerBuffer, "UTF-8").split(LF)
    val Array(http, code, msg) = headerLines.head.split(" ", 3)
    val headers = headerLines.tail.map { l =>
      val header = l.split(":")
      header(0).trim -> header(1).trim
    }.toMap[String, String]

    if (bodyEnd > 0) {
      if (headers.contains("Transfer-Encoding")) {
        headers("Transfer-Encoding") match {
          case "chunked" => {
            if (it.len < 17) {
              None
            }
            val buf = ByteString.newBuilder
            var line = readLine(it)
            var chunkLen = 0|Integer.parseInt(line, 16)
            var chunkFormat = line.length + 4

            while (chunkLen > 0 && it.len > chunkLen) {
              val chunk = new Array[Byte](chunkLen)
              it.getBytes(chunk)
              it.drop(2)
              buf.putBytes(chunk)

              line = readLine(it)
              chunkFormat += line.length + 4
              chunkLen = 0|Integer.parseInt(line, 16)
            }

            if (chunkLen > 0) {
              return None
            }

            it.drop(2)
            totalLen += buf.length+chunkFormat
            bodyBuffer = buf.result().toArray
          }
          case _ => {
            bodyBuffer = new Array[Byte](bodyEnd-bodyStart-4)
            it.getBytes(bodyBuffer)
            totalLen += bodyBuffer.length+4
          }
        }
      } else {
        if (headers.contains("Content-Length")) {
          bodyBuffer = new Array[Byte](bodyEnd-bodyStart-4)
          it.getBytes(bodyBuffer)
          totalLen += bodyBuffer.length+4
        }
      }
    } else if (headers.contains("Transfer-Encoding")) {
      return None
    }

    val body = bodyBuffer
    new Some(ResponseDTO(Response(code.toInt, msg, headers, body), totalLen))
  }

  private def startOut(req:Request):String = {
    req match {
      case Get(host, path, _, _) => "GET "+path+" HTTP/1.1"+CRLF
      case Post(host, path, _, _, _) => "POST "+path+" HTTP/1.1"+CRLF
      case Put(host, path, _, _, _) => "PUT "+path+" HTTP/1.1"+CRLF
      case Delete(host, path, _, _) => "DELETE "+path+" HTTP/1.1"+CRLF
      case Head(host, path, _, _) => "HEAD "+path+" HTTP/1.1"+CRLF
    }
  }

  private def headers(req:Request):String = {
    req.headers.mkString(CRLF)
  }

  @tailrec
  private def findBody(it:ByteIterator, body:StringBuilder, takes:Int):Int = {
    if (it.len < 128) {
      val buffer = new Array[Byte](it.len)
      it.getBytes(buffer)
      body.append(new String(buffer, "UTF-8"))
      return body.result().indexOf(CRLF2)
    } else {
      val buffer = new Array[Byte](128)
      it.getBytes(buffer)
      body.append(new String(buffer, "UTF-8"))
    }

    if (!body.result().contains(CRLF2) && ((takes+1)*128) < it.len) {
      findBody(it, body, takes+1)
    } else {
      body.result().indexOf(CRLF2)
    }
  }

  private def findNeedleInBuffer(buffer:ByteString, needle:String, offset:Int):Int = {
    if (offset < 0) {
      return 0
    }

    val data = buffer.utf8String
    data.indexOf(needle, offset)
  }

  private def readLine(it:ByteIterator):String = {
    val builder = StringBuilder.newBuilder

    var running = true
    while (it.hasNext && running) {
      val b = it.next
      builder.append(b.toByte.toChar)
      running = !b.toChar.equals(CR)
    }

    it.drop(1) // the \n
    val res = builder.result()
    res.substring(0, res.length-1)
  }
}
