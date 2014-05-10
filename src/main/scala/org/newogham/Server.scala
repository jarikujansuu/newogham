package org.newogham

object Server {
	def main(args: Array[String]): Unit = {
		unfiltered.jetty.Http(8080).filter(new Matches).filter(new Leagues).filter(new Teams).run()
	}
}