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

import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import scala.concurrent.Future
import play.api.mvc.Results._
import uk.gov.hmrc.play.http.SessionKeys
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class WithSessionTimeoutAndRedirectUrlSpec extends UnitSpec with WithFakeApplication {

  val redirectToLoginOk = Ok("You just got redirected")
  val host = "localhost"
  val path = "/myUrl/home"

  "WithSessionTimeoutAndRedirectUrl" should {
    "wrap the call in a session timeout and add redirect url" in {
      val myResult = Ok("All good!")
      val headers = FakeHeaders(Seq(("host", Seq(host))))
      implicit val request: FakeRequest[AnyContent] = FakeRequest(method = "GET", uri = path, headers = headers, secure = false, body = AnyContentAsEmpty)

      val result = await(WithSessionTimeoutAndRedirectUrl(authProvider)(Action(myResult))(request))
      result.body shouldBe myResult.body

      val session = result.session
      session.get(SessionKeys.redirect) shouldBe Some(s"http://$host$path")
      session.get(SessionKeys.lastRequestTimestamp) shouldBe defined
    }

    "call redirectToLogin from authProvider if the timestamp has expired" in {
      val passedTimeStamp = "1000"
      val headers = FakeHeaders(Seq(("host", Seq(host))))
      implicit val request = FakeRequest(method = "GET", uri = path, headers = headers, secure = false, body = AnyContentAsEmpty).withSession(SessionKeys.lastRequestTimestamp -> passedTimeStamp)

      val result = await(WithSessionTimeoutAndRedirectUrl(authProvider)(Action(Ok))(request))
      result.body shouldBe redirectToLoginOk.body

      val session = result.session
      session.get(SessionKeys.redirect) shouldBe Some(s"http://$host$path")
      session.get(SessionKeys.lastRequestTimestamp) shouldBe defined
      session.get(SessionKeys.lastRequestTimestamp).get should not be passedTimeStamp
    }

  }
  "buildRedirectUrl" should {

    "build the redirect url from the request for secure protocol" in {
      val headers = FakeHeaders(Seq(("host", Seq(host))))
      val request: FakeRequest[AnyContent] = FakeRequest(method = "GET", uri = path, headers = headers, secure = true, body = AnyContentAsEmpty)
      val url = WithSessionTimeoutAndRedirectUrl.buildRedirectUrl(request)
      url shouldBe s"https://$host$path"
    }

    "build the redirect url from the session for unsecure protocol" in {
      val headers = FakeHeaders(Seq(("host", Seq(host))))
      val request: FakeRequest[AnyContent] = FakeRequest(method = "GET", uri = path, headers = headers, secure = false, body = AnyContentAsEmpty)
      val url = WithSessionTimeoutAndRedirectUrl.buildRedirectUrl(request)
      url shouldBe s"http://$host$path"
    }

    "build the redirect url even if the path is empty" in {
      val request = FakeRequest().withHeaders(("host", host))
      val url = WithSessionTimeoutAndRedirectUrl.buildRedirectUrl(request)
      url shouldBe s"http://$host/"
    }
  }

  "addRedirectUrl" should {
    "add the redirect url to the session" in {
      val headers = FakeHeaders(Seq(("host", Seq(host))))
      implicit val request: FakeRequest[AnyContent] = FakeRequest(method = "GET", uri = path, headers = headers, secure = false, body = AnyContentAsEmpty)
      val result = await(Future.successful(Ok) map WithSessionTimeoutAndRedirectUrl.addRedirectUrl(request))
      result.session.get(SessionKeys.redirect) shouldBe Some(s"http://$host$path")
    }
  }

  object authProvider extends AuthenticationProvider {
    def id: String = ???

    def handleNotAuthenticated(implicit request: Request[_]) = ???

    def redirectToLogin(implicit request: Request[_]): Future[Result] = Future.successful(redirectToLoginOk)
  }

}
