package org.newogham

import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import jk.Futures.enhanceFuture
import jk.RestPlan
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.InternalServerError
import unfiltered.response.Ok
import unfiltered.response.ResponseString
import scala.concurrent.Future
import org.newogham.BB._
import org.joda.time.DateTime

class Matches extends RestPlan {
	implicit val maxWait = 10 seconds

	def intent = {
		case Path(utf8(Seg("matches" :: AsInt(league) :: a :: b :: AsInt(num) :: Nil))) ⇒ {
			BBM.matchBetween(league, a, b, num).waitFor match {
				case Success(m) ⇒ Ok ~> json(m) // img:redirect / json
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
		case Path(utf8(Seg("matches" :: AsInt(league) :: a :: b :: Nil))) ⇒ {
			BBM.matchesBetween(league, a, b).waitFor match {
				case Success(m) ⇒ Ok ~> json(m) // html / json
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
		case Path(Seg("league" :: AsInt(league) :: "matches" :: Nil)) ⇒ {
			BBM.matchesForLeague(league).waitFor match {
				case Success(m) ⇒ Ok ~> json(m)// html / json
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
	}
}

class Teams extends RestPlan {
	implicit val maxWait = 10 seconds

	def intent = {
		case Path(Seg("league" :: AsInt(league) :: "teams" :: Nil)) ⇒ {
			BBM.teamsForLeague(league).waitFor match {
				case Success(t) => Ok ~> json(t)
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
	}
	// team roster from match data?	
	// team roster image? - does it refresh unless created in client?
}

class Leagues extends RestPlan {
	implicit val maxWait = 10 seconds

	def intent = {
		case Path("/leagues") ⇒ {
			BBM.leagues.waitFor match {
				case Success(l) => Ok ~> json(l)
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
		case Path(Seg("league" :: AsInt(league) :: Nil)) ⇒ {
			BBM.league(league).waitFor match {
				case Success(l) => Ok ~> json(l)
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
		case Path(Seg("league" :: AsInt(league) :: "tree" :: Nil)) ⇒ {
			BBM.leagueTree(league).waitFor match {
				case Success(l) => Ok ~> json(l)
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
	}
}