package com.stytch.javamagiclinks;

import com.stytch.java.common.StytchException;
import com.stytch.java.common.StytchResult;
import com.stytch.java.consumer.StytchClient;
import com.stytch.java.consumer.models.magiclinks.AuthenticateRequest;
import com.stytch.java.consumer.models.magiclinks.AuthenticateResponse;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateRequest;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateResponse;
import com.stytch.java.consumer.models.sessions.RevokeRequest;
import com.stytch.java.consumer.models.sessions.RevokeResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@Controller
public class DemoController {
    @GetMapping("/")
    public String index(
        HttpServletRequest request,
        HttpServletResponse res
    ) throws ExecutionException, InterruptedException {
        Cookie[] cookies = getAuthenticationCookiesFromRequest(request);
        Cookie sessionToken = cookies[0];
        Cookie sessionJwt = cookies[1];
        if (sessionToken != null || sessionJwt != null) {
            // Authenticate session
            boolean sessionIsAuthenticated = checkIfSessionIsAuthenticated(sessionToken, sessionJwt);
            if (sessionIsAuthenticated) {
                return "loggedIn";
            } else {
                deleteSessionCookies(res);
            }
        }
        return "index";
    }

    @PostMapping(
        value = "/login_or_create_user",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public String loginOrCreate(String email) throws ExecutionException, InterruptedException, StytchException {
        String MAGIC_LINK_URL = "http://localhost:3000/authenticate";
        LoginOrCreateRequest request = new LoginOrCreateRequest(email, MAGIC_LINK_URL, MAGIC_LINK_URL);
        StytchResult<LoginOrCreateResponse> response = StytchClient.magicLinks.getEmail().loginOrCreateCompletable(request).get();
        if (response instanceof StytchResult.Error) {
            throw ((StytchResult.Error) response).getException();
        }
        return "emailSent";
    }

    @GetMapping("/authenticate")
    public String authenticate(
        @RequestParam("token") String token,
        HttpServletResponse res
    ) throws ExecutionException, InterruptedException, StytchException {
        AuthenticateRequest request = new AuthenticateRequest(token, null, null,null, 30);
        StytchResult<AuthenticateResponse> response = StytchClient.magicLinks.authenticateCompletable(request).get();
        if (response instanceof StytchResult.Error) {
            throw ((StytchResult.Error) response).getException();
        }
        AuthenticateResponse authenticateResponse = ((StytchResult.Success<AuthenticateResponse>) response).getValue();
        Cookie sessionTokenCookie = new Cookie(STYTCH_SESSION_TOKEN, authenticateResponse.getSessionToken());
        Cookie sessionJWTCookie = new Cookie(STYTCH_SESSION_JWT, authenticateResponse.getSessionJwt());
        res.addCookie(sessionTokenCookie);
        res.addCookie(sessionJWTCookie);
        return "loggedIn";
    }

    @GetMapping("/logout")
    public String logout(
        HttpServletRequest request,
        HttpServletResponse res
    ) throws ExecutionException, InterruptedException, StytchException {
        Cookie[] cookies = getAuthenticationCookiesFromRequest(request);
        Cookie sessionToken = cookies[0];
        Cookie sessionJwt = cookies[1];
        if (sessionToken == null && sessionJwt == null) {
            // do nothing, there was no session to revoke
            return "loggedOut";
        }
        // revoke the session
        RevokeRequest revokeRequest;
        if (sessionToken != null) {
            revokeRequest = new RevokeRequest(null, sessionToken.getValue());
        } else {
            revokeRequest = new RevokeRequest(null, null, sessionJwt.getValue());
        }
        StytchResult<RevokeResponse> response = StytchClient.sessions.revokeCompletable(revokeRequest).get();
        if (response instanceof StytchResult.Error) {
            throw ((StytchResult.Error) response).getException();
        }
        // delete the cookies
        deleteSessionCookies(res);
        return "loggedOut";
    }

    private Cookie[] getAuthenticationCookiesFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        Cookie sessionToken = null;
        Cookie sessionJwt = null;
        Cookie[] cookieList = new Cookie[2];
        if (cookies != null) {
            sessionToken = Arrays.stream(cookies).filter(c -> c.getName().equals(STYTCH_SESSION_TOKEN)).findFirst().orElse(null);
            sessionJwt = Arrays.stream(cookies).filter(c -> c.getName().equals(STYTCH_SESSION_JWT)).findFirst().orElse(null);
        }
        if (sessionToken != null) {
            cookieList[0] = sessionToken;
        }
        if (sessionJwt != null) {
            cookieList[1] = sessionJwt;
        }
        return cookieList;
    }

    private boolean checkIfSessionIsAuthenticated(Cookie sessionToken, Cookie sessionJwt) throws ExecutionException, InterruptedException {
        com.stytch.java.consumer.models.sessions.AuthenticateRequest request;
        if (sessionToken != null) {
            request = new com.stytch.java.consumer.models.sessions.AuthenticateRequest(sessionToken.getValue());
        } else {
            request = new com.stytch.java.consumer.models.sessions.AuthenticateRequest(null, null, sessionJwt.getValue());
        }
        StytchResult<com.stytch.java.consumer.models.sessions.AuthenticateResponse> response = StytchClient.sessions.authenticateCompletable(request).get();
        return response instanceof StytchResult.Success<com.stytch.java.consumer.models.sessions.AuthenticateResponse>;
    }

    private void deleteSessionCookies(HttpServletResponse res) {
        Cookie sessionToken = new Cookie(STYTCH_SESSION_TOKEN, null);
        Cookie sessionJwt = new Cookie(STYTCH_SESSION_JWT, null);
        res.addCookie(sessionToken);
        res.addCookie(sessionJwt);
    }

    private static final String STYTCH_SESSION_TOKEN = "stytch_session_token";
    private static final String STYTCH_SESSION_JWT = "stytch_session_jwt";
}
