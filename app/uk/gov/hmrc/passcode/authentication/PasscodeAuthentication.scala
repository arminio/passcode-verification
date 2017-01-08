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
import play.api.mvc._
import uk.gov.hmrc.passcode.authentication.PlayRequestTypes._
import uk.gov.hmrc.passcode.authentication.WithSessionTimeoutAndRedirectUrl._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, AuthenticationProvider}
import uk.gov.hmrc.play.http.{HeaderCarrier, HeaderNames, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import javax.inject.Singleton
import javax.inject.Inject


object PlayRequestTypes {
  type AsyncPlayRequest = (Request[_] => Future[Result])
  type PlayRequest = (Request[_] => Result)
}

trait PasscodeAuthentication {

  def config: PasscodeVerificationConfig
  def passcodeAuthenticationProvider: PasscodeAuthenticationProvider
  lazy val regime = config.regime

  private def invokeAsync(body: PlayRequest)(request: Request[_]): Future[Result] = Future.successful(body(request))

  def PasscodeAuthenticatedAction(body: PlayRequest) = PasscodeAuthenticatedActionAsync(invokeAsync(body))

  def PasscodeAuthenticatedActionAsync(body: => AsyncPlayRequest) = passcodeAuthenticationProvider.ActionAsync(regime, body)

  def withVerifiedPasscode(body: => Future[Result])
                          (implicit request: Request[_], user: AuthContext): Future[Result] = passcodeAuthenticationProvider.verify(regime, body)
}


@Singleton
class PasscodeAuthenticationProvider @Inject() (config: PasscodeVerificationConfig) extends AuthenticationProvider with Results {

  override def id: String = "OTAC"

  override def redirectToLogin(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(config.loginUrl(request)).withNewSession)

  override def handleNotAuthenticated(implicit request: Request[_]) = {
    case _ => Future.successful(Redirect(config.logoutUrl)).map(Right(_))
  }


  def verify(regime: String, body: => Future[Result])(implicit request: Request[_], user: AuthContext): Future[Result] = {
    def failed = Future.successful(Redirect(config.loginUrl(request))) map addRedirectUrl(request)

    if (config.enabled) hasValidBearerToken(regime).flatMap(if (_) body else failed)
    else body
  }

  def ActionAsync(regime: String, body: => AsyncPlayRequest) = {
    if (config.enabled) WithSessionTimeoutAndRedirectUrl(authProvider = this) {
      Action.async { implicit request =>
        hasValidBearerToken(regime).flatMap(if (_) body(request) else redirectToLogin)
      }
    }
    else Action.async(body)
  }

  private[authentication] lazy val authorisationConnector: AuthorisationConnector = AuthorisationConnector

  private[authentication] def hasValidBearerToken(regime: String)(implicit request: Request[_]): Future[Boolean] = {

    def responseToBoolean: WSResponse => Boolean = _.status == 200
    def hcWithOtacAuthorization(otacToken: String) =
      HeaderCarrier
        .fromHeadersAndSession(request.headers, Some(request.session))
        .withExtraHeaders(HeaderNames.otacAuthorization -> otacToken)

    request.session.get(SessionKeys.otacToken).fold(Future.successful(false)) {
      token => authorisationConnector.checkAuthorisationFor(regime)(hcWithOtacAuthorization(token)) map responseToBoolean
    }
  }
}
