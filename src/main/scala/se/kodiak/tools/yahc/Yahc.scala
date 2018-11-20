package se.kodiak.tools.yahc

import org.json4s.DefaultFormats
import scalaj.http.{Http, HttpRequest}

import scala.concurrent.{ExecutionContext, Future}

object Yahc {

	object GET {
		def apply(url:String):HttpRequest = request(url, "GET")
	}

	object POST {
		def apply(url:String, entity:AnyRef):HttpRequest = {
			import org.json4s.native.Serialization.write

			request(url, "POST")
				.postData(write(entity)(DefaultFormats))
		  	.header("Content-Type", "application/json")
		}
	}

	object PUT {
		def apply(url:String, entity:AnyRef):HttpRequest = {
			import org.json4s.native.Serialization.write

			request(url, "PUT")
				.put(write(entity)(DefaultFormats))
				.header("Content-Type", "application/json")
		}
	}

	object DELETE {
		def apply(url:String):HttpRequest = request(url, "DELETE")
	}

	object HEAD {
		def apply(url:String):HttpRequest = request(url, "HEAD")
	}

	def request(url:String, method:String):HttpRequest = Http(url).method(method)

	implicit class Req(req:HttpRequest) {
		def asJson[T](implicit ec:ExecutionContext, tag:Manifest[T]):Future[T] = Future {
			import org.json4s.native.Serialization.read
			read[T](req.asString.body)(DefaultFormats, tag)
		}
	}
}
