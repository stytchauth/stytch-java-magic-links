package com.stytch.stytch_java_magic_links.services;

import com.stytch.java.common.*;
import com.stytch.java.consumer.StytchClient;
import com.stytch.java.consumer.models.magiclinks.AuthenticateRequest;
import com.stytch.java.consumer.models.magiclinks.AuthenticateResponse;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateRequest;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateResponse;
import com.stytch.java.consumer.models.sessions.RevokeRequest;
import com.stytch.java.consumer.models.sessions.RevokeResponse;
import com.stytch.java.consumer.models.sessions.Session;
import com.stytch.java.consumer.models.users.*;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
public class StytchService {
    @Autowired
    private Environment env;

    public static String STYTCH_SESSION_JWT_COOKIE_NAME = "stytch_session_jwt";


    @Bean
    public StytchClient getClient() {
        String id = env.getProperty("stytch.project.id");
        String secret = env.getProperty("stytch.project.secret");
        assert id != null;
        assert secret != null;
        return new StytchClient(id, secret);
    }

    public void loginOrCreate(String email) throws ExecutionException, InterruptedException, StytchException {
        StytchClient client = getClient();
        StytchResult<LoginOrCreateResponse> response = client.magicLinks.getEmail().loginOrCreateCompletable(
                new LoginOrCreateRequest(
                        email,
                        "http://localhost:3000/authenticate",
                        "http://localhost:3000/authenticate"
                )
        ).get();
        if (response instanceof StytchResult.Error) {
            throw ((StytchResult.Error) response).getException();
        }
    }

    public AuthenticateResponse authenticateToken(String token) throws ExecutionException, InterruptedException, StytchException {
        StytchClient client = getClient();
        StytchResult<AuthenticateResponse> response = client.magicLinks.authenticateCompletable(
                new AuthenticateRequest(token, null, null, null, 30)
        ).get();
        if (response instanceof StytchResult.Success<AuthenticateResponse>) {
            return ((StytchResult.Success<AuthenticateResponse>) response).getValue();
        } else {
            throw ((StytchResult.Error) response).getException();
        }
    }

    public Session authenticateJwt(String jwt) throws ExecutionException, InterruptedException, StytchException {
        StytchClient client = getClient();
        StytchResult<JWTResponse> response = client.sessions.authenticateJwtCompletable(jwt, null).get();
        if (response instanceof StytchResult.Success<JWTResponse>) {
            JWTResponse jwtResponse = ((StytchResult.Success<JWTResponse>) response).getValue();
            if (jwtResponse instanceof JWTSessionResponse) {
                return ((JWTSessionResponse) jwtResponse).getResponse();
            } else if (jwtResponse instanceof JWTAuthResponse) {
                return ((JWTAuthResponse) jwtResponse).getResponse().getSession();
            }  else {
                return null;
            }
        } else {
            throw ((StytchResult.Error) response).getException();
        }
    }

    public GetResponse getUser(String userId) throws ExecutionException, InterruptedException, StytchException {
        StytchClient client = getClient();
        StytchResult<GetResponse> getResponse = client.users.getCompletable(new GetRequest(userId)).get();
        if (getResponse instanceof StytchResult.Success<GetResponse>) {
            return ((StytchResult.Success<GetResponse>) getResponse).getValue();
        } else {
            throw ((StytchResult.Error) getResponse).getException();
        }
    }

    public UpdateResponse updateUser(String userId, String firstName, String lastName) throws ExecutionException, InterruptedException, StytchException {
        StytchClient client = getClient();
        StytchResult<UpdateResponse> updateResponse = client.users.updateCompletable(
                new UpdateRequest(
                        userId,
                        new Name(firstName, null, lastName)
                )
        ).get();
        if (updateResponse instanceof StytchResult.Success<UpdateResponse>) {
            return ((StytchResult.Success<UpdateResponse>) updateResponse).getValue();
        } else {
            throw ((StytchResult.Error) updateResponse).getException();
        }
    }

    public void logout() throws ExecutionException, InterruptedException, StytchException {
        StytchClient client = getClient();
        ServletRequestAttributes request = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Optional<Cookie> jwtCookie = Arrays.stream(request.getRequest().getCookies()).filter((Cookie cookie) -> {
            return cookie.getName().equals(STYTCH_SESSION_JWT_COOKIE_NAME);
        }).findFirst();
        if (jwtCookie.isPresent()) {
            StytchResult<RevokeResponse> response = client.sessions.revokeCompletable(
                    new RevokeRequest(null, null, jwtCookie.get().getValue())
            ).get();
            if (response instanceof StytchResult.Success<RevokeResponse>) {
                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(null);
            } else {
                throw ((StytchResult.Error) response).getException();
            }
        }
    }
}
