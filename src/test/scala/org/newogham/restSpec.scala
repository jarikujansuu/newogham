package org.newogham

import org.json4s.jvalue2extractable
import org.newogham.BB._
import org.scalatest.FunSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper

import com.stackmob.newman.dsl.GET

import jk.test.RestClient
import jk.test.RestSuite

class MatchesSpec extends FunSpec with RestSuite with RestClient {
	def api = new Matches
	def encode(a: String) = a.replace(" ", "%20")

	describe("matches rest api") {
		describe("GET /matches/<league>/<a team>/<b team>") {
			it("should return matches") {
				val result = GET(s"/matches/2449/${encode("Eagle Warriors")}/${encode("Kindergarten Nightmares")}").response.json.extract[List[Match]]
				result.isEmpty should be(false)
			}
		}
		describe("GET /matches/<league>/<a team>/<b team>/<num of match>") {
			it("should get correct match") {
				val result = GET(s"/matches/2449/${encode("Eagle Warriors")}/${encode("Kindergarten Nightmares")}/1").response.json.extract[Match]
				result.visitor.name should be("Eagle Warriors")
				result.home.name should be("Kindergarten Nightmares")
				result.id should be(219054)
			}
			it("should get correct match, part 2") {
				val result = GET(s"/matches/2449/${encode("Eagle Warriors")}/${encode("Kindergarten Nightmares")}/2").response.json.extract[Match]
				result.visitor.name should be("Eagle Warriors")
				result.home.name should be("Kindergarten Nightmares")
				result.id should be(248488)
			}
		}
	}
}

class TeamsSpec extends FunSpec with RestSuite with RestClient {
	def api = new Teams
	describe("teams rest api") {
		describe("GET /teams/<league>") {
			ignore("should return league teams") {
				val result = GET("/teams/2449").response
				println("result = " + result)
//				.json.extract[List[Team]]
//				result.isEmpty should be(false)
			}
		}
	}
}

class LeaguesSpec extends FunSpec with RestSuite with RestClient {
	def api = new Leagues

	describe("leagues rest api") {
		describe("GET /leagues") {
			it("should return all leagues") {
				val result = GET("/leagues").response.json.extract[List[League]]
				result.isEmpty should be(false)
			}
		}
		describe("GET /leagues/<league>") {
			it("should return something") {
				GET(s"/leagues/2449").response.json.extract[League]
			}
		}
		describe("GET /leagues/<league>/tree") {
			it("should return league ids for league and all sub leagues") {
				val result = GET("/leagues/2449/tree").response.json.extract[List[Int]]
				result.isEmpty should be(false)
			}
		}
	}
}