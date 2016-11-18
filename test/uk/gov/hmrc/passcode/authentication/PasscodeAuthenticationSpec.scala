/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.passcode.authentication

import org.scalatest.BeforeAndAfterEach
import play.api.Play
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.{HeaderCarrier, HeaderNames, SessionKeys}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

class PasscodeAuthenticationSpec extends UnitSpec with Results with BeforeAndAfterEach {

  def config(authEnabled: Boolean = true) = Map(
    "govuk-tax.Test.services.verification-frontend.host" -> "localhost",
    "govuk-tax.Test.services.verification-frontend.port" -> "9000",
    "govuk-tax.Test.services.auth.host" -> "localhost",
    "govuk-tax.Test.services.auth.port" -> "8500",
    "passcodeAuthentication.enabled" -> s"$authEnabled",
    "passcodeAuthentication.regime" -> "charities")

  val fakeApplication = FakeApplication(additionalConfiguration = config())

  override protected def afterEach(): Unit = {
    super.afterEach()
    Play.stop()
  }


  val GOOD_BEARER_TOKEN: String = "good-bearer-token"
  val expiredTimestamp = DateTimeUtils.now.minusHours(10).getMillis.toString

  object tested extends PasscodeAuthenticationProvider {
    override lazy val authorisationConnector = new AuthorisationConnector {
      override lazy val authBaseUrl = ???
      override def checkAuthorisationFor(regime: String)(implicit hcWithAuthorisation: HeaderCarrier) =
        if (hcWithAuthorisation.extraHeaders.contains((HeaderNames.otacAuthorization, GOOD_BEARER_TOKEN))) {
          Future.successful(new FakeWsResponse(200))
        } else {
          Future.successful(new FakeWsResponse(401))
        }
    }
  }

  val host = "localhost"

  val otac = "12345678"

  val regime = "sa"

  val theResponseBody = Future.successful(Ok("Body of the response"))

  val currentUrl = "/my-url"


  "PasscodeAuthenticatedAction" should {

    "return theResponseBody if auth is disabled" in {

      Play.start(FakeApplication(additionalConfiguration = config(authEnabled = false)))

      val req = FakeRequest("GET", s"$currentUrl?p=$otac")

      val result = await(tested.ActionAsync(regime, _ => theResponseBody).apply(req))
      status(result) shouldBe status(theResponseBody)
      result.body shouldBe theResponseBody.body
    }

    "return theResponseBody if there is a valid bearer token" in {

      Play.start(fakeApplication)

      val req = FakeRequest("GET", s"$currentUrl?p=$otac").withSession(SessionKeys.otacToken -> GOOD_BEARER_TOKEN)

      val result = await(tested.ActionAsync(regime, _ => theResponseBody).apply(req))
      status(result) shouldBe status(theResponseBody)
      result.body shouldBe theResponseBody.body
    }

    "redirect to verification-frontend if the bearer token is invalid" in {
      Play.start(fakeApplication)

      implicit val req = FakeRequest("GET", s"$currentUrl?p=$otac").withHeaders(("host", host)).withSession(SessionKeys.otacToken -> "wrong bearer token")

      val result = await(tested.ActionAsync(regime, _ => theResponseBody).apply(req))
      assertRedirectToVerificationFrontend(result, currentUrl)
    }

    "redirect to verification-frontend if there isn't a bearer token" in {
      Play.start(fakeApplication)

      implicit val req = FakeRequest("GET", s"$currentUrl?p=$otac").withHeaders(("host", host))

      val result = await(tested.ActionAsync(regime, _ => theResponseBody).apply(req))
      assertRedirectToVerificationFrontend(result, currentUrl)
    }

    "redirect to verification-frontend when the session has a correct bearer token but has has expired" in {
      Play.start(fakeApplication)

      implicit val req = FakeRequest("GET", s"$currentUrl?p=$otac").withHeaders(("host", host))
        .withSession(SessionKeys.otacToken -> GOOD_BEARER_TOKEN, SessionKeys.lastRequestTimestamp -> expiredTimestamp)

      val result = await(tested.ActionAsync(regime, _ => theResponseBody).apply(req))
      assertRedirectToVerificationFrontend(result, currentUrl)
    }

    "do not redirect to verification-frontend when the session has a correct bearer token but has has expired, if auth is disabled" in {
      Play.start(FakeApplication(additionalConfiguration = config(authEnabled = false)))

      implicit val req = FakeRequest("GET", s"$currentUrl?p=$otac").withHeaders(("host", host))
        .withSession(SessionKeys.otacToken -> GOOD_BEARER_TOKEN, SessionKeys.lastRequestTimestamp -> expiredTimestamp)

      val result = await(tested.ActionAsync(regime, _ => theResponseBody).apply(req))
      status(result) shouldBe 200
      result.body shouldBe theResponseBody.body
    }
  }

  "withVerifiedPasscode" should {

    "do not redirect to verification-frontend when the session has a correct bearer token but has has expired, if auth is disabled" in {
      Play.start(FakeApplication(additionalConfiguration = config(authEnabled = false)))

      implicit val req = FakeRequest("GET", s"$currentUrl?p=$otac").withHeaders(("host", host))
        .withSession(SessionKeys.otacToken -> GOOD_BEARER_TOKEN, SessionKeys.lastRequestTimestamp -> expiredTimestamp)

      val user = LoggedInUser("test", None, None, None, CredentialStrength.None, ConfidenceLevel.L0)
      val principal = Principal(None, Accounts())
      implicit val authContext = AuthContext(user, principal, None, None, None)

      val result = await(tested.verify(regime, theResponseBody))
      status(result) shouldBe 200
      result.body shouldBe theResponseBody.body
    }
  }

  private def assertRedirectToVerificationFrontend(result: Result, url: String)(implicit req: Request[_]) = {
    status(result) shouldBe 303
    result.session.get(SessionKeys.redirect) shouldBe Some(s"http://$host$url")
  }

  case class FakeWsResponse(status: Int, body: String = "") extends WSResponse {
    override def allHeaders = ???

    override def statusText = ???

    override def underlying[T] = ???

    override def xml = ???

    override def header(key: String) = ???

    override def cookie(name: String) = ???

    override def cookies = ???

    override def json = Json.toJson(body)
  }

}
