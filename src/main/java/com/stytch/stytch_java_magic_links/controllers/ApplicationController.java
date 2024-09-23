package com.stytch.stytch_java_magic_links.controllers;

import com.stytch.java.common.StytchException;
import com.stytch.java.consumer.models.magiclinks.AuthenticateResponse;
import com.stytch.java.consumer.models.users.GetResponse;
import com.stytch.java.consumer.models.users.UpdateResponse;
import com.stytch.stytch_java_magic_links.models.EditForm;
import com.stytch.stytch_java_magic_links.models.LoginForm;
import com.stytch.stytch_java_magic_links.services.StytchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import static com.stytch.stytch_java_magic_links.services.StytchService.STYTCH_SESSION_JWT_COOKIE_NAME;

@Controller
public class ApplicationController {

    @Autowired
    private StytchService authenticationService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/loginOrCreate")
    public String loginOrCreateUser(@ModelAttribute LoginForm loginForm) {
        try {
            authenticationService.loginOrCreate(loginForm.email);
            return "emailSent";
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/authenticate")
    public String authenticate(String token, HttpServletResponse response) {
        try {
            AuthenticateResponse authResponse = authenticationService.authenticateToken(token);
            ResponseCookie cookie = generateJwtCookie(authResponse.getSessionJwt(), 30 * 60);
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return "authenticated";
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            GetResponse getResponse = authenticationService.getUser(authentication.getName());
            request.getSession(true).setAttribute("user", getResponse);
            return "profile";
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/profile")
    public String profile(@ModelAttribute EditForm editForm, HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UpdateResponse updateResponse = authenticationService.updateUser(authentication.getName(), editForm.firstName, editForm.lastName);
            request.getSession(false).setAttribute("user", updateResponse.getUser());
            return "profile";
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/signOut")
    public String signOut(HttpServletResponse response) {
        try {
            authenticationService.logout();
            ResponseCookie cookie = generateJwtCookie("", 0);
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return "loggedOut";
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private String handleException(Exception exception) {
        if (exception instanceof StytchException) {
            System.out.println(((StytchException) exception).getReason());
        } else {
            System.out.println(exception.getMessage());
        }
        return "error";
    }

    private ResponseCookie generateJwtCookie(String value, Integer maxAgeInSeconds) {
        return ResponseCookie.from(STYTCH_SESSION_JWT_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(maxAgeInSeconds)
            .build();
    }
}
