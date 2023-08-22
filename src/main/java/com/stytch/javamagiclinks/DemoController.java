package com.stytch.javamagiclinks;

import com.stytch.java.common.StytchException;
import com.stytch.java.common.StytchResult;
import com.stytch.java.consumer.StytchClient;
import com.stytch.java.consumer.models.magiclinks.AuthenticateRequest;
import com.stytch.java.consumer.models.magiclinks.AuthenticateResponse;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateRequest;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateResponse;
import com.stytch.java.consumer.models.sessions.RevokeRequest;
import com.stytch.java.consumer.models.users.Name;
import com.stytch.java.consumer.models.users.UpdateRequest;
import com.stytch.java.consumer.models.users.UpdateResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@Controller
public class DemoController {
    @GetMapping("/")
    public String index(
        HttpServletRequest request,
        HttpServletResponse res
    ) throws ExecutionException, InterruptedException, IOException {
        Cookie[] cookies = getAuthenticationCookiesFromRequest(request);
        Cookie sessionToken = cookies[0];
        Cookie sessionJwt = cookies[1];
        if (sessionToken != null || sessionJwt != null) {
            // Authenticate session
            boolean sessionIsAuthenticated = getAuthenticatedSession(sessionToken, sessionJwt) instanceof StytchResult.Success<com.stytch.java.consumer.models.sessions.AuthenticateResponse>;
            if (sessionIsAuthenticated) {
                res.sendRedirect("/profile");
            } else {
                res.sendRedirect("/logout");
            }
            return null;
        }
        return "index";
    }

    @GetMapping("/profile")
    public ModelAndView profile(HttpServletRequest request, HttpServletResponse res) throws IOException, ExecutionException, InterruptedException {
        Cookie[] cookies = getAuthenticationCookiesFromRequest(request);
        Cookie sessionToken = cookies[0];
        Cookie sessionJwt = cookies[1];
        if (sessionToken == null && sessionJwt == null) {
            res.sendRedirect("/");
            return null;
        }
        StytchResult<com.stytch.java.consumer.models.sessions.AuthenticateResponse> response = getAuthenticatedSession(sessionToken, sessionJwt);
        if (response instanceof StytchResult.Error) {
            res.sendRedirect("/logout");
            return null;
        }
        ModelAndView mav = new ModelAndView("profile");
        com.stytch.java.consumer.models.sessions.AuthenticateResponse authenticateResponse = ((StytchResult.Success<com.stytch.java.consumer.models.sessions.AuthenticateResponse>) response).getValue();
        mav.addObject("user", authenticateResponse.getUser());
        return mav;
    }

    @PostMapping(
        value = "/profile",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ModelAndView editProfile(HttpServletRequest request, HttpServletResponse res, String firstName, String lastName) throws IOException, ExecutionException, InterruptedException, StytchException {
        Cookie[] cookies = getAuthenticationCookiesFromRequest(request);
        Cookie sessionToken = cookies[0];
        Cookie sessionJwt = cookies[1];
        if (sessionToken == null && sessionJwt == null) {
            res.sendRedirect("/");
            return null;
        }
        StytchResult<com.stytch.java.consumer.models.sessions.AuthenticateResponse> response = getAuthenticatedSession(sessionToken, sessionJwt);
        if (response instanceof StytchResult.Error) {
            res.sendRedirect("/logout");
            return null;
        }
        com.stytch.java.consumer.models.sessions.AuthenticateResponse authenticateResponse = ((StytchResult.Success<com.stytch.java.consumer.models.sessions.AuthenticateResponse>) response).getValue();
        Name name = new Name(firstName, null, lastName);
        UpdateRequest updateRequest = new UpdateRequest(authenticateResponse.getUser().getUserId(), name);
        StytchResult<UpdateResponse> updateResponse = StytchClient.users.updateCompletable(updateRequest).get();
        if (updateResponse instanceof StytchResult.Error) {
            throw ((StytchResult.Error) updateResponse).getException();
        }
        UpdateResponse updateResponseUnwrapped = ((StytchResult.Success<UpdateResponse>) updateResponse).getValue();
        ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", updateResponseUnwrapped.getUser());
        return mav;
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
        return "authenticated";
    }

    @GetMapping("/logout")
    public String logout(
        HttpServletRequest request,
        HttpServletResponse res
    ) throws ExecutionException, InterruptedException {
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
        StytchClient.sessions.revokeCompletable(revokeRequest).get();
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

    private StytchResult<com.stytch.java.consumer.models.sessions.AuthenticateResponse> getAuthenticatedSession(
        Cookie sessionToken,
        Cookie sessionJwt
    ) throws ExecutionException, InterruptedException {
        com.stytch.java.consumer.models.sessions.AuthenticateRequest request;
        if (sessionToken != null) {
            request = new com.stytch.java.consumer.models.sessions.AuthenticateRequest(sessionToken.getValue());
        } else {
            request = new com.stytch.java.consumer.models.sessions.AuthenticateRequest(null, null, sessionJwt.getValue());
        }
        return StytchClient.sessions.authenticateCompletable(request).get();
    }

    private void deleteSessionCookies(HttpServletResponse res) {
        Cookie sessionToken = new Cookie(STYTCH_SESSION_TOKEN, null);
        Cookie sessionJwt = new Cookie(STYTCH_SESSION_JWT, null);
        sessionToken.setMaxAge(0);
        sessionJwt.setMaxAge(0);
        res.addCookie(sessionToken);
        res.addCookie(sessionJwt);
    }

    private static final String STYTCH_SESSION_TOKEN = "stytch_session_token";
    private static final String STYTCH_SESSION_JWT = "stytch_session_jwt";
}
