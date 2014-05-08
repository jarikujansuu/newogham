package jk

import scala.util.Try
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.Await

object Futures {
	implicit def enhanceFuture[A](f: Future[A]): EnhancedFuture[A] = new EnhancedFuture(f)

	class EnhancedFuture[A](f: Future[A]) {
		def waitFor(implicit wait: Duration): Try[A] = Try { Await.result(f, wait) }
	}
}