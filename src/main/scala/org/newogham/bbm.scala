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
import org.joda.time.DateTime
import com.github.nscala_time.time.StaticDateTime
import com.github.nscala_time.time.OrderingImplicits._
import org.scalatest.concurrent.Futures

object BB {
	type Race = String
	case class League(id: Int, name: String, meta: Boolean = false)
	case class LeagueTree(root: League, children: List[LeagueTree])
	case class Team(name: String, coach: String, tv: Int, race: Race)
	case class Match(id: Int, day: Int, home: Team, visitor: Team, homeTD: Int, visitorTD: Int, played: DateTime)
}

object BBM {
	import ExecutionContext.Implicits.global
	import com.stackmob.newman.response.HttpResponseCode._
	implicit val httpClient = new ApacheHttpClient

	private val Host = "bbm.jcmag.fr"
	val BaseUrl = s"http://${Host}/BloodBowlManager.WebSite/"

	def webservice(path: String) = url(http, Host, "/bloodbowlmanager.webservice.public/publicservice.asmx/" + path)
	implicit def enhanceNode(node : Node) = new EnhancedNode(node)
	class EnhancedNode(node : Node) {
		def toInt : Int = if (node.text.length == 0) 0 else node.text.toInt
	}
	
	private def get[A](uri: URL)(parser: String ⇒ A): Future[A] = {
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
		private val Home = "A"
	    private val Visitor = "B"
		
		private def nodeToLeague(node: Node) = {
			val id = (node \ "Id").head.toInt
			val name = (node \ "Name").head.text
			//			val meta = (node \ "IsMetaLeague").head.text.toBoolean
			val meta = true // TODO
			League(id, name, meta)
		}

		def leagueMatches(xml: String): List[Match] = {
			def team(game: Node, postFix: String) = {
				val coach = (game \ s"Coach$postFix").head.text
				val name = (game \ s"Team$postFix").head.text
				val race = (game \ s"Race$postFix").head.text
				val tv = (game \ s"TV$postFix").head.toInt
				Team(name, coach, tv, race)
			}
			
			(XML.loadString(xml) \\ "LeagueMatch").map({ game ⇒
				val id = (game \ "Id").head.toInt
				val day = (game \ "Day").head.toInt
				val home = team(game, Home)
				val visitor = team(game, Visitor)
				val homeTd = (game \ s"Score$Home").head.toInt
				val visitorTd = (game \ s"Score$Visitor").head.toInt
				val date = (game \ "Date").map(_.text).map(DateTime.parse(_)).head
				
				Match(id, day, home, visitor, homeTd, visitorTd, date)
			}).toList.sortBy(_.day)
		}

		def league(xml: String): League = (XML.loadString(xml) \\ "LeagueEntity").map(nodeToLeague).head
		def leagues(xml: String): List[League] = (XML.loadString(xml) \\ "LeagueEntity").map(nodeToLeague).toList
		def metaLeagueChildren(xml: String): List[Int] = (XML.loadString(xml) \\ "MetaLeagueConfig").map(c ⇒ { (c \ "LeagueId").head.toInt }).toList
	}

	def leagues: Future[List[League]] = get(webservice("GetAllLeagues")) { Xml.leagues }

	def leagueTree(root: Int) = {
		def children(parent : Int) = get(webservice("GetMetaChildren") ? (("parentLeagueId", parent.toString))) { Xml.metaLeagueChildren }
		
		children(root).flatMap({
			case c if c.isEmpty => Future.successful(c)
			case c => Future.sequence(c.map(children(_))).map(c ++ _.flatten)
		})
	}
	def leagueAndChildren(root : Int) = leagueTree(root).map(children => root :: children)	
	
	def toLeagueTree(root: League): Future[LeagueTree] = {
		def toLeaguesTree(parents: List[League]): Future[List[LeagueTree]] = Future.sequence(parents.map(toLeagueTree))
		def childrenOf(parent: League): Future[List[League]] = {
			if (parent.meta) leagueTree(parent.id).flatMap(children ⇒ Future.sequence(children.map(league)))
			else Future.successful(Nil)
		}
		childrenOf(root).flatMap(toLeaguesTree).map(LeagueTree(root, _))
	}

	def league(id: Int): Future[League] = get(webservice("GetLeague") ? (("leagueId", id.toString))) { Xml.league }

	def teamsForLeague(id: Int): Future[Set[Team]] = ???

	def matchesForLeague(id: Int) = {
		leagueAndChildren(id).flatMap(leagues ⇒{
			val matchesByLeague = leagues.map(id ⇒ { get(webservice("GetMatchsByLeague") ? (("leagueId", id.toString))) { Xml.leagueMatches } })
			Future.sequence(matchesByLeague).map(_.flatten)
		})
	}

	def matchesBetween(league: Int, aTeam: String, bTeam: String): Future[List[Match]] = {
		def isPlaying(team: String, in: Match) = team == in.home.name || team == in.visitor.name

		matchesForLeague(league).map(_.filter(m ⇒ (isPlaying(aTeam, m) && isPlaying(bTeam, m)))).map(_.sortBy(_.played))
	}
	def matchBetween(league: Int, aTeam: String, bTeam: String, num : Int = 1): Future[Option[Match]] = matchesBetween(league, aTeam, bTeam).map(_.lift(num - 1))
}
