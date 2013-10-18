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
import akka.util.Timeout

object HttpClient {
  def apply():HttpClient = {
    new HttpClient
  }
}

// TODO add reconnect method.
// TODO add support for many connections / client.
class HttpClient {
  val connections = mutable.Map[String, ActorRef]()
  val system = ActorSystem("yahc-actor-system")

  def send(req:Request):Future[Response] = {
    val connector = lookupAddress(req.server)

    import system.dispatcher
    import java.util.concurrent.TimeUnit
    val promise = Promise[Response]()

    implicit val timeout = Timeout(15, TimeUnit.SECONDS) // TODO extract to property
    val response = connector ? new Operation(req, promise)

    promise.completeWith(response.mapTo[Response])

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

case class Operation(req:Request, promise:Promise[Response])
case class Response(status:Int, message:String, headers:Map[String, String], body:Array[Byte])

private class HttpClientActor(val host:InetSocketAddress) extends Actor {
  implicit val system = context.system
  IO(Tcp) ! Connect(host)

  val responseQue = mutable.Queue[Promise[Response]]()
  val requestQue = mutable.Queue[Request]()

  def receive = {
    // TODO react to connection closed, and reconnect.
    case CommandFailed(cmd:Connect) => {
      // TODO what to do? Fail all promises and suicide?
      println("Connect failed.")
    }
    case con:Connected => {
      val connection = sender
      sender ! Register(self)

      if (!requestQue.isEmpty) {
        connection ! Write(HttpUtil(requestQue.dequeue()))
      }

      context become {
        case Received(data) => {
          val promise = responseQue.dequeue()
          promise.complete(Success(HttpUtil(data))) // TODO this potentially assumes each frame are a full response...
          // Solve by returning Option[Response] in HttpUtil and add a buffer here and try the whole buffer until it works?

          if (!requestQue.isEmpty) {
            connection ! Write(HttpUtil(requestQue.dequeue()))
          }
        }
        case op:Operation => {
          val parent = sender
          val promise = op.promise
          responseQue.enqueue(promise) // TODO this can grow out of control, add limit controlled from config
          connection ! Write(HttpUtil(op.req), Ack)
          parent ! promise.future
        }
      }
    }
    case op:Operation => {
      val parent = sender
      responseQue.enqueue(op.promise) // TODO this can grow out of control, add limit controlled from config
      requestQue.enqueue(op.req)
    }
  }
  // TODO add more to come Commandfailed(which_command)
}

private object Ack extends Event
