package org.newogham

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import BBM._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._

class bbmSpec extends FunSpec with ScalaFutures {
	describe("league") {
		whenReady(league(2449), timeout(1 seconds)) { result =>
			result.id should be (2449)
			result.name should be ("NOBBL")
			result.meta should be (true)
		}
	}
	describe("metaLeagues") {
		whenReady(metaLeague(2449), timeout(30 seconds)) { result =>
			result.league.id should be (2449)
			result.league.name should be ("NOBBL")
			result.league.meta should be (true)
			result.children.isEmpty should be (false)
			println(s"children {\n ${result.children.mkString("\n")} \n")
		}
	}
}