package football

import common.ExecutionContexts
import org.scalatest.{DoNotDiscover, Matchers, FreeSpec}
import play.api.test._
import play.api.test.Helpers._
import football.services.GetPaClient
import test.ConfiguredTestSuite

@DoNotDiscover class SiteControllerTest extends FreeSpec with GetPaClient with ExecutionContexts with Matchers with ConfiguredTestSuite {

  "test index page loads" in {
    val Some(result) = route(FakeRequest(GET, "/admin/football"))
    status(result) should equal(OK)
  }
}
