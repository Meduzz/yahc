package se.kodiak.utils.yahc

import se.kodiak.utils.yahc.DSL.Request
import scala.concurrent.{Promise, Future}
import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import scala.util.Success
import java.net.InetSocketAddress
import akka.pattern.ask
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import scala.collection.mutable
import akka.util.{ByteString, Timeout}

object HttpClient {
  def apply():HttpClient = {
    new HttpClient
  }
}

// TODO add reconnect method.
class HttpClient {
  val connections = mutable.Map[String, ActorRef]()
  val system = ActorSystem("yahc-actor-system")

  def send(req:Request):Future[Response] = {
    val connector = lookupAddress(req.server)

    import system.dispatcher
    import java.util.concurrent.TimeUnit
    val promise = Promise[Response]()

    println(req.getClass.getSimpleName)

    implicit val timeout = Timeout(15, TimeUnit.SECONDS) // TODO extract to property
    connector ! new RequestDTO(req, promise)

    promise.future
  }

  def disconnect(host:InetSocketAddress) = {
    lookupAddress(host) ! Close // TODO will try to create a socket to close if it have too...
  }

  private def lookupAddress(host:InetSocketAddress):ActorRef = {
    if (!connections.contains(host.getHostName+":"+host.getPort)) {
      connections += (host.getHostName+":"+host.getPort -> system.actorOf(Props(classOf[HttpClientActor], host), host.getHostName+":"+host.getPort))
    }
    connections(host.getHostName+":"+host.getPort)
  }
}

case class RequestDTO(req:Request, promise:Promise[Response])
case class Response(status:Int, message:String, headers:Map[String, String], body:Array[Byte])
case class Append(data:ByteString)
case class ResponseDTO(res:Response, length:Int)

private class HttpClientActor(val host:InetSocketAddress) extends Actor {
  implicit val system = context.system
  IO(Tcp) ! Connect(host)

  val requestQue = mutable.Queue[Request]()
  val responseHandler = system.actorOf(Props[HttpResponseActor])

  def receive = {
    // TODO react to connection closed, and reconnect.
    case CommandFailed(cmd:Connect) => {
      // TODO what to do? Fail all promises and suicide?
      println("Connect failed.")
    }
    case con:Connected => {
      val connection = sender
      sender ! Register(self)

      while (!requestQue.isEmpty) {
        connection ! Write(HttpUtil(requestQue.dequeue()))
      }

      context become {
        case Received(data) => {
          responseHandler ! Append(data)

          if (!requestQue.isEmpty) {
            connection ! Write(HttpUtil(requestQue.dequeue()))
          }
        }
        case op:RequestDTO => {
          responseHandler ! op.promise // TODO this can grow out of control, add limit controlled from config
          connection ! Write(HttpUtil(op.req), Ack)
        }
      }
    }
    case op:RequestDTO => {
      responseHandler ! op.promise // TODO this can grow out of control, add limit controlled from config
      requestQue.enqueue(op.req)
    }
  }
  // TODO add more to come Commandfailed(which_command)
}

private object Ack extends Event

private class HttpResponseActor extends Actor {
  val responseQue = mutable.Queue[Promise[Response]]()
  val buffer = ByteString.newBuilder

  def receive = {
    case Append(data) => {
      buffer append data
      val workingBuffer = buffer.result()
      HttpUtil(workingBuffer) match {
        case None => {}
        case Some(r:ResponseDTO) => {
          buffer.clear()
          buffer.append(workingBuffer.drop(r.length))

          if (buffer.length > 0) {
            context.self ! Append(ByteString.newBuilder.result())
          }

          val promise = responseQue.dequeue()
          promise.complete(Success(r.res))
        }
      }
    }
    case f:Promise[Response] => {
      responseQue.enqueue(f)
    }
  }
}
