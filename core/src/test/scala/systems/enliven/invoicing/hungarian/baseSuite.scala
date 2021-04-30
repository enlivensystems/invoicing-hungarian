package systems.enliven.invoicing.hungarian

import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, CancelAfterFailure, TestSuite}
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}

trait baseSuite
 extends TestSuite with Matchers with BeforeAndAfterAll with CancelAfterFailure with Logger {

  implicit protected val configuration: Configuration = new Configuration()

  def eventually[U](f: => U)(implicit timeout: Span = Span(60, Seconds)): U =
    Eventually.eventually(Eventually.timeout(timeout), Eventually.interval(Span(2, Seconds))) {
      logException(f)
    }

  protected def logException[U](f: => U): U =
    try {
      f
    } catch {
      case t: Throwable =>
        log.error(s"Eventually not satisfied due to [${t.getMessage}] of [${t.getClass.getName}]!")
        throw t
    }

  def eventually[U](timeout: Span, interval: Span)(f: => U): U =
    Eventually.eventually(Eventually.timeout(timeout), Eventually.interval(interval)) {
      logException(f)
    }

}
