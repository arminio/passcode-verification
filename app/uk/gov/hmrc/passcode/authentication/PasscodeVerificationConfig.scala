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

import uk.gov.hmrc.play.config.ServicesConfig
import play.api.mvc.Request
import play.api.Play
import play.api.Play.current

class PasscodeVerificationException(msg: String) extends RuntimeException(msg)

object PasscodeVerificationConfig extends ServicesConfig {

  private[authentication] val tokenParam = "p"
  private val passcodeEnabledKey = "passcodeAuthentication.enabled"
  private val passcodeRegimeKey = "passcodeAuthentication.regime"

  def enabled: Boolean = Play.configuration.getBoolean(passcodeEnabledKey).getOrElse(throwConfigNotFound(passcodeEnabledKey))

  def regime: String = Play.configuration.getString(passcodeRegimeKey).getOrElse(throwConfigNotFound(passcodeRegimeKey))

  def getVerificationURL() = Play.current.configuration.getString(s"govuk-tax.$env.url.verification-frontend.redirect").getOrElse("/")

  def loginUrl(request: Request[_]) = s"$getVerificationURL/otac/login${tokenQueryParam(request)}"

  def logoutUrl = s"$getVerificationURL/otac/logout/$regime"

  private def tokenQueryParam(request: Request[_]):String =
   request.getQueryString(tokenParam).map(token => s"?$tokenParam=$token").getOrElse("")

  private def throwConfigNotFound(configKey:String) = throw new PasscodeVerificationException(s"The value for the key '$configKey' should be setup in the config file.")
}
