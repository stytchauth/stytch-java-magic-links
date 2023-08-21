package com.stytch.javamagiclinks;

import com.stytch.java.common.StytchException;
import com.stytch.java.common.StytchResult;
import com.stytch.java.consumer.StytchClient;
import com.stytch.java.consumer.models.magiclinks.AuthenticateRequest;
import com.stytch.java.consumer.models.magiclinks.AuthenticateResponse;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateRequest;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.concurrent.ExecutionException;

@Controller
public class DemoController {
    @GetMapping("/")
    public String index() {
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
    public String authenticate(@RequestParam("token") String token) throws ExecutionException, InterruptedException, StytchException {
        AuthenticateRequest request = new AuthenticateRequest(token);
        StytchResult<AuthenticateResponse> response = StytchClient.magicLinks.authenticateCompletable(request).get();
        if (response instanceof StytchResult.Error) {
            throw ((StytchResult.Error) response).getException();
        }
        return "loggedIn";
    }

    @GetMapping("/logout")
    public String logout() throws ExecutionException, InterruptedException, StytchException {
        /* Actually log the user out
        RevokeRequest request = new RevokeRequest(...);
        StytchResult<RevokeResponse> response = StytchClient.sessions.revokeCompletable(request).get();
        if (response instanceof StytchResult.Error) {
            throw ((StytchResult.Error) response).getException();
        }
        */
        return "loggedOut";
    }
}
