package jk

import scala.language.implicitConversions
import scala.util.Either
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.Exception.allCatch
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue
import org.json4s.MappingException
import org.json4s.ParserUtil.ParseException
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods
import org.json4s.native.JsonMethods.compact
import org.json4s.native.JsonMethods.render
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import unfiltered.filter.Plan
import unfiltered.request.Body
import unfiltered.request.HttpRequest
import unfiltered.response.BadRequest
import unfiltered.response.InternalServerError
import unfiltered.response.JsonContent
import unfiltered.response.Ok
import unfiltered.response.ResponseFunction
import unfiltered.response.ResponseString
import unfiltered.response.Status
import com.typesafe.scalalogging.slf4j.LazyLogging

import java.net.URLDecoder
import java.nio.charset.Charset
import Http._

object Http {
	type Request = HttpRequest[HttpServletRequest]
	type Response = ResponseFunction[HttpServletResponse]	
}

trait RestPlan extends Plan with LazyLogging {
	implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

	trait Extract {
		def charset: Charset
		def unapply(raw: String) = Try(URLDecoder.decode(raw, charset.name())).toOption
	}
	object utf8 extends Extract { val charset = Charset.forName("utf8") }
	object AsInt { def unapply(s: String): Option[Int] = allCatch.opt(s.toInt) }
	def parse(req: Request) = JsonMethods.parse(Body.string(req))

	def json(input: AnyRef) = JsonContent ~> ResponseString(write(input))


	def DefaultResponseHandler[A <: AnyRef] = { r: A ⇒ Ok ~> json(r) }

	lazy val DefaultFailureHandler = {
		e: Throwable ⇒
			e match {
				case e: HttpErrorStatusException ⇒
					logger.warn(s"Request failed due to ${e.status} with message '${e.getMessage}'")
					e.status ~> ResponseString(e.getMessage)
				case ex ⇒
					logger.error("Failed to handle request.", ex)
					InternalServerError ~> ResponseString(e.getMessage)
			}
	}

	def handleJson[A, B <: AnyRef](
		handler: A ⇒ Try[B],
		responseHandler: B ⇒ Response = DefaultResponseHandler,
		failureHandler: Throwable ⇒ Response = DefaultFailureHandler)(req: Request)(implicit m: Manifest[A]) = {

		def handleInput(in: A) = handler(in) match {
			case Success(resp) ⇒ responseHandler(resp)
			case Failure(e: HttpErrorStatusException) ⇒
				logger.warn(s"Request ${req.method} ${req.uri} failed due to ${e.status}")
				e.status
			case Failure(e) ⇒ failureHandler(e)
		}

		parseJson(req).fold(BadRequest ~> ResponseString(_), handleInput)
	}

	def handleJsonNoResponse[A](
		handler: A ⇒ Try[Any],
		response: ⇒ Response = Ok,
		failureHandler: Throwable ⇒ Response = DefaultFailureHandler)(req: Request)(implicit m: Manifest[A]) = {

		type NoResponse = AnyRef
		def handlerWrapper: A ⇒ Try[NoResponse] = { a: A ⇒ handler(a).map(x ⇒ None) }
		def responseWrapper = { noresponse: NoResponse ⇒ response }

		handleJson[A, NoResponse](handlerWrapper, responseWrapper)(req)
	}

	/**
	  * Parse Json input from request.
	  * @return Either right containing object of type <code>A</code> parsed from request, or left containing parse error string.
	  */
	def parseJson[A](req: Request)(implicit m: Manifest[A]): Either[String, A] = {
		Try(parse(req).extract[A]) match {
			case Success(a) ⇒ Right(a)
			case Failure(e: MappingException) ⇒
				logger.warn(s"Request did not contain valid JSON in body.\n${e.getMessage}")
				Left(s"Request did not contain valid JSON in body.\n${e.getMessage}")
			case Failure(e: ParseException) ⇒
				logger.warn(s"Request did not contain JSON in body.\n${e.getMessage}")
				Left(s"Request did not contain JSON in body.\n${e.getMessage}")
			case Failure(e) ⇒
				logger.warn(s"Unknown error in parsing json.\n${e.getMessage}")
				Left(s"Unknown error in parsing json.\n${e.getMessage}")
		}
	}

	object HttpErrorStatus {
		def apply(msg: String, s: Status) = new HttpErrorStatusException(msg, s)
		def apply(s: Status) = new HttpErrorStatusException(s)

		val NotImplemented = Failure(apply(unfiltered.response.NotImplemented))
		val BadRequest = Failure(apply(unfiltered.response.BadRequest))
	}
	class HttpErrorStatusException(msg: String, val status: Status) extends Throwable(msg) {
		def this(status: Status) = this(null, status)
	}
}