package se.kodiak.tools.yahc

import org.json4s.DefaultFormats
import scalaj.http.{Http, HttpRequest, HttpResponse}

object Yahc {

	type Signer = (String, HttpRequest)=>HttpRequest
	val NoBody = ""

	object GET {
		def apply(url:String, signer:Option[Signer] = None):HttpRequest = {
			val req = request(url, "GET")

			signer.map(sign => sign(NoBody, req))
		  	.getOrElse(req)
		}
	}

	object POST {
		def apply(url:String, entity:AnyRef, signer:Option[Signer] = None):HttpRequest = {
			import org.json4s.native.Serialization.write

			val body = write(entity)(DefaultFormats)
			val req = request(url, "POST")
				.postData(body)
		  	.header("Content-Type", "application/json")

			signer.map(sign => sign(body, req))
		  	.getOrElse(req)
		}
	}

	object PUT {
		def apply(url:String, entity:AnyRef, signer:Option[Signer] = None):HttpRequest = {
			import org.json4s.native.Serialization.write

			val body = write(entity)(DefaultFormats)
			val req = request(url, "PUT")
				.put(body)
				.header("Content-Type", "application/json")

			signer.map(sign => sign(body, req))
		  	.getOrElse(req)
		}
	}

	object DELETE {
		def apply(url:String, signer:Option[Signer] = None):HttpRequest = {
			val req = request(url, "DELETE")

			signer.map(sign => sign(NoBody, req))
		  	.getOrElse(req)
		}
	}

	object HEAD {
		def apply(url:String, signer:Option[Signer] = None):HttpRequest = {
			val req = request(url, "HEAD")

			signer.map(sign => sign(NoBody, req))
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
