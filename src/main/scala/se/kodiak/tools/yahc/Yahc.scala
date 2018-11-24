package se.kodiak.tools.yahc

import org.json4s.DefaultFormats
import scalaj.http.{Http, HttpRequest, HttpResponse}

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
