package com.stytch;

import com.stytch.java.common.StytchResult;
import com.stytch.java.consumer.StytchClient;
import com.stytch.java.consumer.models.magiclinks.AuthenticateRequest;
import com.stytch.java.consumer.models.magiclinksemail.LoginOrCreateRequest;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class StytchServlet extends HttpServlet {
    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException {
        try {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                index(req, res);
            } else if (path.equals("/authenticate")) {
                authenticate(req, res);
            } else if (path.equals("/logout")) {
                logout(req, res);
            } else {
                throw new ServletException("Unknown Route");
            }
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException {
        try {
            String path = req.getPathInfo();
            String MAGIC_LINK_URL = "http://" + Application.HOST + ":" + Application.PORT + "/demo/authenticate";
            if (path != null && path.equals("/login_or_create_user")) {
                String email = req.getParameter("email");
                var request = new LoginOrCreateRequest(
                        email,
                        MAGIC_LINK_URL,
                        MAGIC_LINK_URL
                );
                var response = StytchClient.magicLinks.getEmail().loginOrCreateCompletable(request).get();
                if (response instanceof StytchResult.Error) {
                    var exception = ((StytchResult.Error) response).getException();
                    throw new ServletException(exception);
                }
                RequestDispatcher view = req.getRequestDispatcher("/templates/emailSent.html");
                view.forward(req, res);
            } else {
                throw new ServletException("Unknown Route");
            }
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    public void index(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException, IOException {
        RequestDispatcher view = req.getRequestDispatcher("/templates/loginOrSignup.html");
        view.forward(req, res);
    }

    public void authenticate(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException, IOException, ExecutionException, InterruptedException {
        String tokenType = req.getParameter("stytch_token_type");
        String token = req.getParameter("token");
        if (tokenType == null) {
            throw new ServletException("missing token type");
        }
        if (!tokenType.equals("magic_links")) {
            throw new ServletException("unsupported token type");
        }
        if (token == null) {
            throw new ServletException("missing token");
        }
        var response = StytchClient.magicLinks.authenticateCompletable(
            new AuthenticateRequest(token)
        ).get();
        if (response instanceof StytchResult.Error) {
            var exception = ((StytchResult.Error) response).getException();
            throw new ServletException(exception);
        }
        System.out.println(response);
        RequestDispatcher view = req.getRequestDispatcher("/templates/loggedIn.html");
        view.forward(req, res);
    }

    public void logout(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException, IOException {
        // Logging the user out depends on how you choose to persist the session information after authentication
        RequestDispatcher view = req.getRequestDispatcher("/templates/loggedOut.html");
        view.forward(req, res);
    }
}
