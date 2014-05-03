package org.newogham

import java.io.IOException
import java.net.URL
import scala.concurrent._
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Node
import scala.xml.XML
import com.stackmob.newman.ApacheHttpClient
import com.stackmob.newman.dsl._
import com.stackmob.newman.dsl.URLBuilderDSL
import BB._
import scala.concurrent.duration.Duration

case class League(id: Int, name: String, meta: Boolean = false)
case class MetaLeague(league : League, children : List[MetaLeague])
case class Team(name: String, coach: String, tv: Int, race: Race)
case class Match(league: Int, day: Int, home: Team, visitor: Team)

object BB {
	type Race = Int
}

object BBM {
	import ExecutionContext.Implicits.global
	import com.stackmob.newman.response.HttpResponseCode._
	implicit val httpClient = new ApacheHttpClient

	private val Host = "bbm.jcmag.fr"
	
	def webservice(path : String) = url(http, Host, "/bloodbowlmanager.webservice.public/publicservice.asmx/" + path) 
	
	private def get[A](uri : URL)(parser: String ⇒ A): Future[A] = {
		val p = Promise[A]
		GET(uri).apply.map(resp ⇒ {
			resp.code match {
				case Ok ⇒ Try(parser(resp.bodyString)) match {
					case Success(r) ⇒ p.success(r)
					case Failure(e) ⇒ p.failure(e)
				}
				case _ ⇒ Failure(new IOException)
			}
		})
		p.future
	}

	private object Xml {
		private def nodeToLeague(node : Node) = {
			val id = (node \ "Id").head.text.toInt
			val name = (node \ "Name").head.text
//			val meta = (node \ "IsMetaLeague").head.text.toBoolean
			val meta = true // TODO
			League(id, name, meta)
		}
		
		def league(xml : String) : League = (XML.loadString(xml) \\ "LeagueEntity").map(nodeToLeague).head
		def leagues(xml : String) : List[League] = (XML.loadString(xml) \\ "LeagueEntity").map(nodeToLeague).toList 
		def metaLeagueChildren(xml : String) : List[Int] = (XML.loadString(xml) \\ "MetaLeagueConfig").map(c ⇒ { (c \ "LeagueId").head.text.toInt }).toList 
	}
	
	def leagues: Future[List[League]] = get(webservice("GetAllLeagues")) { Xml.leagues }

	def leagueTree(root : Int) = get(webservice("GetMetaChildren") ? (("parentLeagueId", root.toString))) { Xml.metaLeagueChildren }

	def toMetaLeague(root: League): Future[MetaLeague] = {
		def toMetaLeagues(parents : List[League]) : Future[List[MetaLeague]] = Future.sequence(parents.map(toMetaLeague))
		def childrenOf(parent : League) : Future[List[League]]= {
			if (parent.meta) leagueTree(parent.id).flatMap(children => Future.sequence(children.map(league)))
			else Future.successful(Nil)
		}				
		childrenOf(root).flatMap(toMetaLeagues).map(MetaLeague(root, _))
	}

	def league(id: Int) : Future[League] = get(webservice("GetLeague") ? (("leagueId", id.toString))) { Xml.league }
	
	def teamsForLeague(id: Int): Set[Team] = ???
	def matchesForLeague(id: Int) = ???
}
