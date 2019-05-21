/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.oidc;

import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.jwt.impl.SecurityAssertionJwt;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.codice.ddf.security.handler.api.OidcAuthenticationToken;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.callback.QueryParameterCallbackUrlResolver;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.credentials.extractor.OidcExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcHandler implements AuthenticationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcHandler.class);

  private static final String SOURCE = "OidcHandler";
  private static final String AUTH_TYPE = "OIDC";

  private static HandlerResult noActionResult;
  private static HandlerResult redirectedResult;

  static {
    noActionResult = new HandlerResult(Status.NO_ACTION, null);
    noActionResult.setSource(SOURCE);

    redirectedResult = new HandlerResult(Status.REDIRECTED, null);
    redirectedResult.setSource(SOURCE);
  }

  private OidcHandlerConfiguration configuration;
  private SessionFactory sessionFactory;

  public OidcHandler(OidcHandlerConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  /**
   * Handler implementing OIDC authentication.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return result of handling this request - status and optional tokens
   * @throws AuthenticationFailureException
   */
  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
      throws AuthenticationFailureException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    if (httpRequest.getMethod().equals("HEAD")) {
      return processHeadRequest(httpResponse);
    }

    HttpSession session = getOrCreateSessionOnRequest(httpRequest);
    if (session == null) {
      LOGGER.error("Unable to get/create session off of incoming request. Cannot continue.");
      return noActionResult;
    }
    SecurityTokenHolder tokenHolder = getOrCreateTokenHolderOnSession(session);
    if (tokenHolder == null) {
      LOGGER.error("Unable to get/create token holder off of session. Cannot continue.");
      return noActionResult;
    }

    J2ESessionStore sessionStore = new J2ESessionStore();
    J2EContext j2EContext = new J2EContext(httpRequest, httpResponse, sessionStore);

    // credentials exist on session
    if (tokenHolder.getPrincipals() != null
        && tokenHolder.getPrincipals() instanceof PrincipalCollection) {
      return getCredentialsFromTokenHolder(tokenHolder, session, j2EContext);
    }

    // at this point, the OIDC Handler must be configured
    if (!configuration.isInitialized()) {
      LOGGER.error(
          "The OIDC Handler's configuration has not been initialized. "
              + "Configure \"OIDC Handler Configuration\" in the admin console to initialize.");
      return noActionResult;
    }

    // no credentials found in session, time to try and pull some off of the request
    LOGGER.debug(
        "Doing Oidc authentication and authorization for path {}.", httpRequest.getContextPath());

    OidcCredentials credentials = new OidcCredentials();

    StringBuffer requestUrlBuffer = httpRequest.getRequestURL();
    requestUrlBuffer.append(
        httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString());
    String requestUrl = requestUrlBuffer.toString();
    String ipAddress = httpRequest.getRemoteAddr();

    configuration.getOidcClient().setCallbackUrl(requestUrl);

    boolean isMachine = userAgentIsNotBrowser(httpRequest);

    if (isMachine) {
      LOGGER.debug(
          "The Oidc Handler does not handle machine to machine requests. Continuing to other handlers.");
      return noActionResult;
    } else { // check for Authorization Code Flow, Implicit Flow, or Hybrid Flow credentials
      try {
        credentials = getCredentials(j2EContext);
      } catch (IllegalArgumentException e) {
        LOGGER.debug(e.getMessage(), e);
        LOGGER.error(
            "Problem with the Oidc Handler's configuration. "
                + "Check the Oidc Handler configuration in the admin console.");
        return noActionResult;
      } catch (TechnicalException e) {
        LOGGER.debug("Problem extracting Oidc credentials from incoming user request.", e);
      }
    }

    // if the request has credentials, process it
    if (credentials.getCode() != null
        || credentials.getAccessToken() != null
        || credentials.getIdToken() != null) {
      LOGGER.info(
          "Oidc credentials found/retrieved. Saving to session and continuing filter chain.");
      OidcAuthenticationToken token =
          new OidcAuthenticationToken(credentials, j2EContext, ipAddress);

      HandlerResult handlerResult = new HandlerResult(Status.COMPLETED, token);
      handlerResult.setSource(SOURCE);
      return handlerResult;
    } else { // the user agent request didn't have credentials, redirect and go get some
      LOGGER.info(
          "No credentials found on user-agent request. "
              + "Redirecting user-agent to IdP for credentials.");
      return redirectForCredentials(j2EContext, requestUrl);
    }
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    LOGGER.debug("In error handler for Oidc - no action taken.");
    return noActionResult;
  }

  public OidcHandlerConfiguration getConfiguration() {
    return configuration;
  }

  private HandlerResult processHeadRequest(HttpServletResponse httpResponse)
      throws AuthenticationFailureException {
    httpResponse.setStatus(HttpServletResponse.SC_OK);
    try {
      httpResponse.flushBuffer();
    } catch (IOException e) {
      throw new AuthenticationFailureException(
          "Unable to send response to HEAD message from OIDC client.");
    }
    return noActionResult;
  }

  private HandlerResult getCredentialsFromTokenHolder(
      SecurityTokenHolder tokenHolder, HttpSession session, J2EContext j2EContext) {
    // guaranteed non null PrincipalCollection by calling code
    PrincipalCollection principals = (PrincipalCollection) tokenHolder.getPrincipals();

    OidcCredentials credentials =
        (OidcCredentials)
            principals
                .byType(SecurityAssertion.class)
                .stream()
                .filter(sa -> SecurityAssertionJwt.JWT_TOKEN_TYPE.equals(sa.getTokenType()))
                .map(SecurityAssertion::getToken)
                .findFirst()
                .orElse(null);

    if (credentials == null) {
      LOGGER.debug(
          "No Oidc Credentials found in token holder principals. Continuing to other handlers.");
      return noActionResult;
    }

    if ((credentials.getCode() == null
        && credentials.getAccessToken() == null
        && credentials.getIdToken() == null)) {
      LOGGER.error(
          "Invalid Oidc Credentials found in token holder principals, invalidating session and continuing to other handlers.",
          credentials);
      session.invalidate();
      return noActionResult;
    }

    OidcAuthenticationToken token =
        new OidcAuthenticationToken(credentials, j2EContext, j2EContext.getRemoteAddr());

    HandlerResult handlerResult = new HandlerResult(Status.COMPLETED, token);
    handlerResult.setSource(SOURCE);
    return handlerResult;
  }

  private boolean userAgentIsNotBrowser(HttpServletRequest httpRequest) {
    String userAgentHeader = httpRequest.getHeader("User-Agent");
    // basically all browsers support the "Mozilla" way of operating, so they all have "Mozilla"
    // in the string. I just added the rest in case that ever changes for existing browsers.
    // New browsers should contain "Mozilla" as well, though.
    return userAgentHeader == null
        || !(userAgentHeader.contains("Mozilla")
            || userAgentHeader.contains("Safari")
            || userAgentHeader.contains("OPR")
            || userAgentHeader.contains("MSIE")
            || userAgentHeader.contains("Edge")
            || userAgentHeader.contains("Chrome"));
  }

  private OidcCredentials getCredentials(J2EContext j2EContext) {
    configuration.getOidcClient().setCallbackUrlResolver(new QueryParameterCallbackUrlResolver());

    OidcExtractor oidcExtractor =
        new OidcExtractor(configuration.getOidcConfiguration(), configuration.getOidcClient());
    return oidcExtractor.extract(j2EContext);
  }

  private HandlerResult redirectForCredentials(J2EContext j2EContext, String requestUrl) {
    j2EContext.getSessionStore().set(j2EContext, Pac4jConstants.REQUESTED_URL, requestUrl);

    configuration.getOidcClient().redirect(j2EContext);

    return redirectedResult;
  }

  private HttpSession getOrCreateSessionOnRequest(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    if (session == null) {
      session = sessionFactory.getOrCreateSession(httpRequest);
    }
    return session;
  }

  private SecurityTokenHolder getOrCreateTokenHolderOnSession(HttpSession session) {
    SecurityTokenHolder tokenHolder =
        ((SecurityTokenHolder)
            session.getAttribute(ddf.security.SecurityConstants.SECURITY_TOKEN_KEY));
    if (tokenHolder == null) {
      tokenHolder = new SecurityTokenHolder();
      session.setAttribute(ddf.security.SecurityConstants.SECURITY_TOKEN_KEY, tokenHolder);
    }
    return tokenHolder;
  }

  // hack

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  // end hack
}
