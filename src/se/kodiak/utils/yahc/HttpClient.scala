package se.kodiak.utils.yahc

import se.kodiak.utils.yahc.DSL.{Get, Post, Put, Delete, Head, Request}
import scala.concurrent.{Promise, Future}
import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import scala.collection.mutable
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import scala.util.Failure
import akka.io.Tcp.CommandFailed
import java.net.InetSocketAddress

/*
 In the best of worlds you'd like to have an actor as a connection and send it Get/Post/Puts etc, and then
 reuse that "connection" with different builder.
 That means, either split host+port from request or redo the DSL.
 */

object HttpClient {

  def apply(req:Request):Future[Response] = {
    null
  }

}

class HttpClient(val host:InetSocketAddress) {
  val system = ActorSystem("yahc-actor-system")

  val connector = system.actorOf(HttpClientActorProps.props(host), "HttpClientActorProps")

  def send(req:Request):Future[Response] = {
    val promise = Promise[Response]()

    promise.future
  }
}

case class Response(status:Int, message:String, headers:Map[String, String], body:Array[Byte])

private object HttpClientActorProps {
  def props(host:InetSocketAddress):Props = Props(classOf[HttpClientActor], host)
}

private class HttpClientActor(val host:InetSocketAddress) extends Actor {
  implicit val system = context.system
  IO(Tcp) ! Connect(host)
  var canWrite = true
  var writeQueue = mutable.Queue[Request]()
  var promises = mutable.Queue[Promise[Response]]()
  var connection:ActorRef = null

  def receive = {
    case CommandFailed(cmd:Connect) => {
      promises.dequeue().complete(Failure(new RuntimeException("Connection failed with: "+cmd.failureMessage)))
      context stop self
    }
    // TODO more to come Commandfailed(which_command)
    case con:Connected => {
      connection = sender
      connection ! Register(self)

      // TODO we'll need a helper here to transform to and from bytestringish.
      context become writing
    }
  }

  def writing:Receive = {
    // case Request <- request ;)
    case req:Request => {
      val promise = Promise[Response]()
      sender ! promise.future

      promises enqueue promise
      connection ! Write(null, Ack)
    }
  }

  def reading:Receive = {
    // case ByteString <- response
    case "stfu" => println("stfu")
  }
}

private object Ack extends Event
