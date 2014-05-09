package org.newogham

import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

import jk.Futures.enhanceFuture
import jk.Http.Request
import jk.RestPlan
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.BadRequest
import unfiltered.response.InternalServerError
import unfiltered.response.NotFound
import unfiltered.response.NotImplemented
import unfiltered.response.Ok
import unfiltered.response.Redirect
import unfiltered.response.ResponseString

trait NewOghamApi {
	import jk.Http._
	
	implicit val maxWait = 10 seconds
	
	val DefaultFormat = "json"
	val Json = "json"
	val Html = "html"
	val Img = "img"
		
	def output(req : Request) = {
		val json = req.parameterNames.contains(Json)
		val html = req.parameterNames.contains(Json)
		val img = req.parameterNames.contains(Img)
		
		(json, html, img) match {
			case (true, false, false) => Json
			case (false, true, false) => Html
			case (false, false, true) => Img
			case _ => DefaultFormat
		}
	}
}

class Matches extends RestPlan with NewOghamApi {
	def intent = {
		case req @ Path(utf8(Seg("matches" :: AsInt(league) :: a :: b :: AsInt(num) :: Nil))) ⇒ {
			BBM.matchBetween(league, a, b, num).waitFor match {
				case Success(Some(m)) ⇒ output(req) match {
					case Json => Ok ~> json(m)
					case Img => Redirect(s"${BBM.BaseUrl}/ImageMatchReport.aspx?Id=${m.id}&lang=en") 
					case f => BadRequest ~> ResponseString(s"Unsupported format '$f'")
				}
				case Success(None) => NotFound ~> ResponseString("No match found between teams.")
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
		case req @ Path(utf8(Seg("matches" :: AsInt(league) :: a :: b :: Nil))) ⇒ {
			BBM.matchesBetween(league, a, b).waitFor match {
				case Success(m) ⇒ output(req) match {
					case Json => Ok ~> json(m) // html / json
					case Html => NotImplemented
					case f => BadRequest ~> ResponseString(s"Unsupported format '$f'")
				}
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
		case req @ Path(Seg("league" :: AsInt(league) :: "matches" :: Nil)) ⇒ {
			BBM.matchesForLeague(league).waitFor match {
				case Success(m) ⇒ output(req) match {
					case Json => Ok ~> json(m)
					case Html => NotImplemented
				}
				case Failure(e) ⇒ InternalServerError ~> ResponseString(e.getMessage)
			}
		}
	}
}

class Teams extends RestPlan with NewOghamApi {
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

class Leagues extends RestPlan with NewOghamApi {
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