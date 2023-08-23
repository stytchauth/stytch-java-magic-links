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
import com.stytch.java.consumer.models.users.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Controller
public class DemoController {
    @GetMapping("/")
    public ModelAndView index(@NotNull HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("index");
        User authenticatedUser = (User) request.getAttribute("authenticatedUser");
        mav.addObject("user", authenticatedUser);
        return mav;
    }

    @GetMapping("/profile")
    public ModelAndView profile(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response
    ) throws IOException {
        User authenticatedUser = (User) request.getAttribute("authenticatedUser");
        if (authenticatedUser == null) {
            response.sendRedirect("/");
            return null;
        }
        ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", authenticatedUser);
        return mav;
    }

    @PostMapping(
        value = "/profile",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ModelAndView editProfile(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        String firstName,
        String lastName
    ) throws IOException, ExecutionException, InterruptedException, StytchException {
        User authenticatedUser = (User) request.getAttribute("authenticatedUser");
        if (authenticatedUser == null) {
            response.sendRedirect("/");
            return null;
        }
        Name name = new Name(firstName, null, lastName);
        UpdateRequest updateRequest = new UpdateRequest(authenticatedUser.getUserId(), name);
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
        @NotNull HttpServletRequest request,
        @RequestParam("token") String token
    ) throws StytchException, ExecutionException, InterruptedException {
        AuthenticateRequest stytchRequest = new AuthenticateRequest(token, null, null,null, 30);
        StytchResult<AuthenticateResponse> stytchResponse = StytchClient.magicLinks.authenticateCompletable(stytchRequest).get();
        if (stytchResponse instanceof StytchResult.Error) {
            throw ((StytchResult.Error) stytchResponse).getException();
        }
        AuthenticateResponse authenticateResponse = ((StytchResult.Success<AuthenticateResponse>) stytchResponse).getValue();
        StytchCookies cookies = new StytchCookies();
        cookies.sessionToken = authenticateResponse.getSessionToken();
        cookies.jwt = authenticateResponse.getSessionJwt();
        request.setAttribute("stytchCookies", cookies);
        return "authenticated";
    }

    @GetMapping("/logout")
    public String logout(@NotNull HttpServletRequest request) throws ExecutionException, InterruptedException {
        StytchCookies stytchCookies = (StytchCookies) request.getAttribute("stytchCookies");
        if (stytchCookies != null) {
            RevokeRequest revokeRequest = new RevokeRequest(null, stytchCookies.sessionToken, stytchCookies.jwt);
            StytchClient.sessions.revokeCompletable(revokeRequest).get();
            stytchCookies.sessionToken = null;
            stytchCookies.jwt = null;
            request.setAttribute("stytchCookies", stytchCookies);
        }
        return "loggedOut";
    }
}
