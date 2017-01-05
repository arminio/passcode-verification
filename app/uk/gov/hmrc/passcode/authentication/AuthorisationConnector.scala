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

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.connectors.Connector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait AuthorisationConnector extends Connector with ServicesConfig {

  val action = "read"

  def authBaseUrl: String

  private def authoriseBearerUrl(regime: String) = s"$authBaseUrl/authorise/$action/$regime"

  def checkAuthorisationFor(regime: String)(implicit hcWithAuthorisation: HeaderCarrier): Future[WSResponse] =
    buildRequest(authoriseBearerUrl(regime)).get()
}

object AuthorisationConnector extends AuthorisationConnector {

  override lazy val authBaseUrl = baseUrl("auth")
}
