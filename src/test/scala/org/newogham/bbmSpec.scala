package org.newogham

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import BB._
import BBM._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._

class bbmSpec extends FunSpec with ScalaFutures {
	describe("leagues") {
		it("should return all leagues") {
			whenReady(leagues, timeout(10 seconds)) { result ⇒
				result.isEmpty should be (false)
			}
		}
	}
	describe("league") {
		it("should return correct league") {
			whenReady(league(2449), timeout(10 seconds)) { result ⇒
				result.id should be(2449)
				result.name should be("NOBBL")
				result.meta should be(true)
			}
		}
	}
	describe("toLeagueTree") {
		it("should transform league into league tree") {
			whenReady(toLeagueTree(League(2449, "NOBBL", true)), timeout(10 seconds)) { result ⇒
				result.root should be (League(2449, "NOBBL", true))
				result.children.isEmpty should be (false)
			}
		}
	}
	describe("matchesForLeague") {
		it("should get all matches in league") {
			whenReady(matchesForLeague(2449), timeout(1000 seconds)) { result =>
				println(result)
				result.isEmpty should be (false)
			}
		}
	}
	describe("matchesBetween") {
		it("should get all matches in league between teams") {
			whenReady(matchesBetween(2449, "Eagle Warriors", "Kindergarten Nightmares"), timeout(1000 seconds)) { result =>
				println(result)
				result.isEmpty should be (false)
			}
		}
	}
}