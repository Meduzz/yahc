package se.kodiak.utils.yahc

import se.kodiak.utils.yahc.DSL.{Get, Post, Put, Delete, Head, Request}
import scala.concurrent.{Promise, Future}
import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import scala.util.{Try, Success, Failure}
import akka.io.Tcp.CommandFailed
import java.net.InetSocketAddress
import akka.pattern.ask
import akka.io.Tcp.Connected
import se.kodiak.utils.yahc.Response
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import scala.collection.mutable

/*
 In the best of worlds you'd like to have an actor as a connection and send it Get/Post/Puts etc, and then
 reuse that "connection" with different builder.
 That means, either split host+port from request or redo the DSL.
 */

object HttpClient {

  def apply(host:InetSocketAddress):HttpClient = {
    new HttpClient(host)
  }

}

// TODO add reconnect method.
class HttpClient(val host:InetSocketAddress) {
  val system = ActorSystem("yahc-actor-system")
  val connector = system.actorOf(Props(classOf[HttpClientActor], host), "HttpClientActorProps")

  def send(req:Request):Future[Response] = {
    val promise = Promise[Response]()

    import system.dispatcher
    val response = connector ? req
    promise.completeWith(response.mapTo[Response])

    promise.future
  }

  def disconnect = {
    connector ! Close
  }
}

case class Response(status:Int, message:String, headers:Map[String, String], body:Array[Byte])

private class HttpClientActor(val host:InetSocketAddress) extends Actor {
  implicit val system = context.system
  IO(Tcp) ! Connect(host)

  def receive = {
    case CommandFailed(cmd:Connect) => {
      // TODO what to do?
    }
    // TODO more to come Commandfailed(which_command)
    case con:Connected => {
      val handler = system.actorOf(Props(classOf[HttpHandler], sender, self))
      sender ! Register(handler)

      context become {
        case req:Request => {
          val parent = sender
          val promise = Promise[Response]()

          import context.dispatcher
          val response = handler ? req
          promise.completeWith(response.mapTo[Response])

          parent ! promise.future
        }
        case Close => handler ! Close // TODO is this enough, or do we need a poisonpill as well?
      }
    }
  }
}

private class HttpHandler(val connection:ActorRef, val parent:ActorRef) extends Actor {
  import Tcp._

  context watch connection
  val que = mutable.Queue[Promise[Response]]()


  def receive = {
    // TODO add a couple of CommandFailed versions.
    case req:Request => {
      val promise = Promise[Response]()
      que.enqueue(promise) // TODO this can grow out of control
      sender ! promise.future
      connection ! Write(HttpUtil(req), Ack)
    }
    case Received(data) => {
      val promise = que.dequeue()
      promise.complete(Success(HttpUtil(data))) // TODO this assumes each frame are a full response...
      // Solve by returning Option[Response] in HttpUtil and add a buffer here and try the whole buffer until it works?
    }
    case PeerClosed => context stop self
  }
}

private object Ack extends Event
