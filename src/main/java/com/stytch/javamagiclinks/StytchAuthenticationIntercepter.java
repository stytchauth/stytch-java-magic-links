package com.stytch.javamagiclinks;

import com.stytch.java.common.StytchResult;
import com.stytch.java.consumer.StytchClient;
import com.stytch.java.consumer.models.sessions.AuthenticateRequest;
import com.stytch.java.consumer.models.sessions.AuthenticateResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import java.util.Arrays;

@Component
public class StytchAuthenticationIntercepter implements HandlerInterceptor {
    @Override
    public boolean preHandle(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull Object handler
    ) throws Exception {
        Cookie[] cookies = request.getCookies();
        Cookie sessionToken = null;
        Cookie sessionJwt = null;
        if (cookies != null) {
            sessionToken = Arrays.stream(cookies).filter(c -> c.getName().equals(STYTCH_SESSION_TOKEN)).findFirst().orElse(null);
            sessionJwt = Arrays.stream(cookies).filter(c -> c.getName().equals(STYTCH_SESSION_JWT)).findFirst().orElse(null);
        }
        if (sessionToken != null || sessionJwt != null) {
            AuthenticateRequest stytchAuthenticateRequest;
            if (sessionToken != null) {
                stytchAuthenticateRequest = new AuthenticateRequest(sessionToken.getValue());
            } else {
                stytchAuthenticateRequest = new AuthenticateRequest(null, null, sessionJwt.getValue());
            }
            StytchResult<AuthenticateResponse> stytchAuthenticateResponse = StytchClient.sessions.authenticateCompletable(stytchAuthenticateRequest).get();
            StytchCookies stytchCookies = new StytchCookies();
            if (stytchAuthenticateResponse instanceof StytchResult.Error) {
                request.setAttribute("authenticatedUser", null);
                stytchCookies.sessionToken = null;
                stytchCookies.jwt = null;
            } else {
                AuthenticateResponse authenticateResponse = ((StytchResult.Success<AuthenticateResponse>) stytchAuthenticateResponse).getValue();
                request.setAttribute("authenticatedUser", authenticateResponse.getUser());
                stytchCookies.sessionToken = authenticateResponse.getSessionToken();
                stytchCookies.jwt = authenticateResponse.getSessionJwt();
            }
            request.setAttribute("stytchCookies", stytchCookies);
        }
        return true;
    }

    @Override
    public void postHandle(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull Object handler,
        @Nullable ModelAndView modelAndView
    ) {
        StytchCookies cookies = (StytchCookies) request.getAttribute("stytchCookies");
        if (cookies != null) {
            Cookie sessionTokenCookie = new Cookie(STYTCH_SESSION_TOKEN, cookies.sessionToken);
            Cookie sessionJwtCookie = new Cookie(STYTCH_SESSION_JWT, cookies.jwt);
            if (sessionTokenCookie.getValue() == null) {
                sessionTokenCookie.setMaxAge(0);
            }
            if (sessionJwtCookie.getValue() == null) {
                sessionJwtCookie.setMaxAge(0);
            }
            response.addCookie(sessionTokenCookie);
            response.addCookie(sessionJwtCookie);
        }
    }

    private static final String STYTCH_SESSION_TOKEN = "stytch_session_token";
    private static final String STYTCH_SESSION_JWT = "stytch_session_jwt";
}
