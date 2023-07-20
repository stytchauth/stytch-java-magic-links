package com.stytch;

import com.stytch.kotlin.consumer.StytchClient;
import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import java.io.File;
import java.io.IOException;
import java.net.URL;

class Application {
    public static final String HOST = "localhost";
    public static final Integer PORT = 3000;
    private static final Dotenv dotenv = Dotenv.configure().filename("local.properties").load();

    public static void main(String[] args) {
        StytchClient.INSTANCE.configure(dotenv.get("STYTCH_PROJECT_ID"), dotenv.get("STYTCH_PROJECT_SECRET"));
        try {
            new Application().start();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public void start() throws Exception {
        URL baseDir = Application.class.getResource("/webroot/");
        assert baseDir != null;

        Server server = new Server(PORT);
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(),"stytch-java-magic-links");

        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setAttribute("javax.servlet.context.tempdir",scratchDir);
        context.setResourceBase(baseDir.toURI().toASCIIString());

        ServletHolder holderIndexer = new ServletHolder(new StytchServlet());
        context.addServlet(holderIndexer,"/demo/*");
        context.setWelcomeFiles(new String[]{ "demo" });
        ServletHolder holderDefault = new ServletHolder("default",DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase",baseDir.getFile());
        holderDefault.setInitParameter("dirAllowed","true");
        holderDefault.setInitParameter("welcomeServlets","true");
        holderDefault.setInitParameter("redirectWelcome","true");

        context.addServlet(holderDefault,"/");

        server.setHandler(context);

        server.start();
        server.join();
    }
}
