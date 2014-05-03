package org.newogham

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import BB._
import BBM._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._

class bbmSpec extends FunSpec with ScalaFutures {
	describe("league") {
		it("should return correct league") {
			whenReady(league(2449), timeout(1 seconds)) { result ⇒
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
}