package org.newogham

import org.joda.time.DateTime

object BB {
	type Race = String
	case class League(id: Int, name: String, meta: Boolean = false)
	case class LeagueTree(root: League, children: List[LeagueTree])
	case class Team(name: String, coach: String, tv: Int, race: Race)
	case class Match(id: Int, day: Int, home: Team, visitor: Team, homeTD: Int, visitorTD: Int, played: DateTime)
}

