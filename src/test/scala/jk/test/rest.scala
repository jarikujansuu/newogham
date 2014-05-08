package jk.test

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

import com.stackmob.newman.ApacheHttpClient
import com.stackmob.newman.dsl.HeaderAndBodyBuilder
import com.stackmob.newman.dsl.HeaderBuilder
import com.stackmob.newman.dsl.http
import com.stackmob.newman.dsl.stringToPath
import com.stackmob.newman.dsl.transformerToHttpRequest
import com.stackmob.newman.dsl.url
import com.stackmob.newman.request.HttpRequest
import com.stackmob.newman.response.HttpResponse

import unfiltered.filter.Plan
import unfiltered.jetty.Http

trait RestSuite extends Suite with RestClient with BeforeAndAfter with BeforeAndAfterAll {
  var server: Option[Http] = None
  implicit var testPort = 0;
  def api: Plan
  
  
  override def beforeAll = {
    super.beforeAll()
    server = Some(unfiltered.jetty.Http.anylocal.filter(api))
    testPort = server.get.port
    server.get.start
  }

  override def afterAll = {
    super.afterAll()
    server.foreach(_.stop())
  }
}

trait RestClient {
  import org.json4s._
  import org.json4s.native._
  import scala.language.implicitConversions
  import com.stackmob.newman._
  import com.stackmob.newman.dsl._
  import org.json4s.DefaultFormats
  import org.json4s.JsonMethods

  implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  implicit val httpClient = new ApacheHttpClient
  def path(uri: String)(implicit testPort : Int) = url(http, "localhost", testPort, uri)
  def response(request: HttpRequest, duration: Duration = Duration.Inf) = Await.result(request.apply, duration)

  implicit def stringToPath(uri: String)(implicit testPort : Int) = path(uri).toURL

  class TestHttpRequest(val request: HttpRequest) {
    def response = Await.result(request.apply, Duration.Inf)
  }
  implicit def headerBuilderToRequest(request: HeaderBuilder) = new TestHttpRequest(request)
  implicit def headerAndBodyBuilderToRequest(builder : HeaderAndBodyBuilder) = new TestHttpRequest(builder.toRequest) 
  
  class TestHttpResponse(val response: HttpResponse) {
    // TODO extract given class? how...
    def json = JsonMethods.parse(response.bodyString)
  }
  implicit def responseToTestResponse(response: HttpResponse) = new TestHttpResponse(response)

  def toJson(a : AnyRef) = write(a)
}

