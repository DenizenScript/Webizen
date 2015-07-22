package space.morphanone.webizen.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.scheduler.BukkitRunnable;
import space.morphanone.webizen.Webizen;
import space.morphanone.webizen.events.GetRequestScriptEvent;
import space.morphanone.webizen.events.PostRequestScriptEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

public class WebServer {

    private static HttpServer httpServer;

    public static void start(int port) throws IOException {
        if (!isRunning()) {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        GetRequestScriptEvent.instance.fire(httpExchange);
                    }
                    //else if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    //    PostRequestScriptEvent.instance.fire(httpExchange);
                    //}
                }
            });
            httpServer.setExecutor(new Executor() {
                @Override
                public void execute(final Runnable command) {
                    command.run();
                }
            });
            httpServer.start();
        }
    }

    public static void stop() {
        if (isRunning()) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    public static boolean isRunning() {
        return httpServer != null;
    }
}
