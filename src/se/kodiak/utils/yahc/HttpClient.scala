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

  def apply(host:InetSocketAddress):HttpClient = { // TODO change this to take a config
    new HttpClient(host)
  }

}

// TODO add reconnect method.
// TODO add support for many connections / client.
class HttpClient(val host:InetSocketAddress) {
  val system = ActorSystem("yahc-actor-system")
  val connector = system.actorOf(Props(classOf[HttpClientActor], host), "HttpClientActor")

  def send(req:Request):Future[Response] = { // TODO look up connections on host:ip and send request.
    import system.dispatcher
    import java.util.concurrent.TimeUnit
    val promise = Promise[Response]()

    implicit val timeout = Timeout(15, TimeUnit.SECONDS) // TODO extract to property
    val response = connector ? new Operation(req, promise)

    promise.completeWith(response.mapTo[Response])

    promise.future
  }

  def disconnect = {
    connector ! Close
  }
}

case class Operation(req:Request, promise:Promise[Response])
case class Response(status:Int, message:String, headers:Map[String, String], body:Array[Byte])

private class HttpClientActor(val host:InetSocketAddress) extends Actor {
  implicit val system = context.system
  IO(Tcp) ! Connect(host)

  val responseQue = mutable.Queue[Promise[Response]]()
  val requestQue = mutable.MutableList[Request]()

  def receive = {
    case CommandFailed(cmd:Connect) => {
      // TODO what to do? Fail all promises and suicide?
      println("Connect failed.")
    }
    case con:Connected => {
      val connection = sender
      sender ! Register(self)

      requestQue.foreach { r =>
        connection ! Write(HttpUtil(r), Ack)
      }
      requestQue.clear()

      context become {
        case Received(data) => {
          val promise = responseQue.dequeue()
          promise.complete(Success(HttpUtil(data))) // TODO this potentially assumes each frame are a full response...
          // Solve by returning Option[Response] in HttpUtil and add a buffer here and try the whole buffer until it works?
        }
        case req:Request => {
          val parent = sender
          val promise = Promise[Response]()
          responseQue.enqueue(promise) // TODO this can grow out of control, add limit controlled from config
          connection ! Write(HttpUtil(req), Ack)
          parent ! promise.future
        }
      }
    }
    case op:Operation => {
      val parent = sender
      responseQue.enqueue(op.promise) // TODO this can grow out of control, add limit controlled from config
      requestQue += op.req
    }
  }
  // TODO add more to come Commandfailed(which_command)
}

private object Ack extends Event
