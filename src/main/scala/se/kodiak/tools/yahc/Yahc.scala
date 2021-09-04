package se.kodiak.tools.yahc

import org.json4s.DefaultFormats
import scalaj.http.{Http, HttpRequest, HttpResponse}

object Yahc {

	type BodyHook = (String, HttpRequest)=>HttpRequest
	val NoBody = ""

	object GET {
		def apply(url:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "GET")
				.header("Accept", "application/json")

			bodyHook.map(hook => hook(NoBody, req))
		  	.getOrElse(req)
		}

		def text(url:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "GET")
				.header("Accept", "text/plain")

			bodyHook.map(hook => hook(NoBody, req))
				.getOrElse(req)
		}
	}

	object POST {
		def apply(url:String, entity:AnyRef, bodyHook:Option[BodyHook] = None):HttpRequest = {
			import org.json4s.native.Serialization.write

			val body = write(entity)(DefaultFormats)
			val req = request(url, "POST")
				.postData(body)
		  	.header("Content-Type", "application/json")
		  	.header("Accept", "application/json")

			bodyHook.map(hook => hook(body, req))
		  	.getOrElse(req)
		}

		def text(url:String, data:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "POST")
				.postData(data)
				.header("Content-Type", "text/plain")
				.header("Accept", "text/plain")

			bodyHook.map(hook => hook(data, req))
				.getOrElse(req)
		}
	}

	object PUT {
		def apply(url:String, entity:AnyRef, bodyHook:Option[BodyHook] = None):HttpRequest = {
			import org.json4s.native.Serialization.write

			val body = write(entity)(DefaultFormats)
			val req = request(url, "PUT")
				.put(body)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")

			bodyHook.map(hook => hook(body, req))
		  	.getOrElse(req)
		}

		def text(url:String, data:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "PUT")
				.put(data)
				.header("Content-Type", "text/plain")
				.header("Accept", "text/plain")

			bodyHook.map(hook => hook(data, req))
				.getOrElse(req)
		}
	}

	object DELETE {
		def apply(url:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "DELETE")
				.header("Accept", "application/json")

			bodyHook.map(hook => hook(NoBody, req))
		  	.getOrElse(req)
		}

		def text(url:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "DELETE")
				.header("Accept", "text/plain")

			bodyHook.map(hook => hook(NoBody, req))
				.getOrElse(req)
		}
	}

	object HEAD {
		def apply(url:String, bodyHook:Option[BodyHook] = None):HttpRequest = {
			val req = request(url, "HEAD")

			bodyHook.map(hook => hook(NoBody, req))
				.getOrElse(req)
		}
	}

	def request(url:String, method:String):HttpRequest = Http(url).method(method)

	implicit class StringRes(res:HttpResponse[String]) {
		def asJson[T](implicit tag:Manifest[T]):T = {
			import org.json4s.native.Serialization.read
			read[T](res.body)(DefaultFormats, tag)
		}
	}

	implicit class BytesRes(res:HttpResponse[Array[Byte]]) {
		def asJson[T](implicit tag:Manifest[T]):T = {
			import org.json4s.native.Serialization.read
			read[T](new String(res.body, "utf-8"))(DefaultFormats, tag)
		}
	}
}
