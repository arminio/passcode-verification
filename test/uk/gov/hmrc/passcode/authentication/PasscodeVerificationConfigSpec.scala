/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.{FakeApplication, FakeRequest}
import org.scalatest.BeforeAndAfterEach
import play.api.Play

class PasscodeVerificationConfigSpec extends UnitSpec with BeforeAndAfterEach {

  val host = "localhost"
  val port = "9227"
  val regime = "charities"

  val configWithoutKeys = Map(
    "govuk-tax.Test.services.verification-frontend.host" -> host,
    "govuk-tax.Test.services.verification-frontend.port" -> port)

  val config = configWithoutKeys + ("passcodeAuthentication.enabled" -> "true") + ("passcodeAuthentication.regime" -> regime)
  var app: Option[FakeApplication] = None

  def fakeApplication(withConfig: Map[String, String] = config) = {
    app = Some(FakeApplication(additionalConfiguration = withConfig))
    app.get
  }

  override def afterEach() {
    super.afterEach()
    app.map(Play.stop)
  }

  "Calling enable on PasscodeVerificationConfig" should {
    "return the value from the config" in {
      Play.start(fakeApplication(config))
      PasscodeVerificationConfig.enabled shouldBe true
    }

    "throw an exception if it could not find it in the config" in {
      Play.start(fakeApplication(configWithoutKeys))

      intercept[PasscodeVerificationException] {
        PasscodeVerificationConfig.enabled
      }.getMessage should not be empty

    }
  }

  "Calling regime on PasscodeVerificationConfig" should {
    "return the value from the config" in {
      Play.start(fakeApplication(config))
      PasscodeVerificationConfig.regime shouldBe regime
    }

    "throw an exception if it could not find it in the config" in {
      Play.start(fakeApplication(configWithoutKeys))

      intercept[PasscodeVerificationException] {
        PasscodeVerificationConfig.regime
      }.getMessage should not be empty

    }
  }

  "Calling url on PasscodeVerificationConfig" should {
    "build and return the verification url" in {
      Play.start(fakeApplication(config))

      val otac = "1234"
      val req = FakeRequest("GET", s"/my-url?p=$otac")

      PasscodeVerificationConfig.loginUrl(req) shouldBe s"http://$host:$port/verification/otac/login?${PasscodeVerificationConfig.tokenParam}=$otac"
    }
    "build a login url without a token if there is no query param with the user token in the request" in {
      Play.start(fakeApplication(config))

      val req = FakeRequest("GET", s"/my-url")

      PasscodeVerificationConfig.loginUrl(req) shouldBe s"http://$host:$port/verification/otac/login"
    }
  }
}
