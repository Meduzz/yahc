package se.kodiak.tools.yahc

import java.util.concurrent.TimeUnit

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.duration.Duration

trait Server extends BeforeAndAfterAll { self:FunSuite =>

	implicit private val duration = Duration(1L, TimeUnit.SECONDS)

	private val vertx = Vertx.vertx()
	private val router = Router.router(vertx)
	private val server = vertx.createHttpServer()
		.requestHandler(router)
	private val bodyhandler = BodyHandler.create(false)

	router.get("/:name/:age")
  		.blockingHandler(ctx => {
			  val name = ctx.pathParam("name")
			  val age = ctx.pathParam("age").toInt

			  ctx.end(s"""{"name":"$name", "age":$age}""")
		  })

	router.post("/post/echo")
  	  .handler(bodyhandler.handle)
	    .blockingHandler(ctx => {
		    val body = ctx.getBodyAsString
			  ctx.end(body)
		  })

	router.put("/put/echo")
		.handler(bodyhandler.handle)
		.blockingHandler(ctx => {
			val body = ctx.getBodyAsString
			ctx.end(body)
		})

	override protected def beforeAll():Unit = server.listen(6006)

	override protected def afterAll():Unit = server.close()
}
