package space.morphanone.webizen.server;

import com.sun.net.httpserver.HttpServer;
import space.morphanone.webizen.events.GetRequestScriptEvent;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {

    private static HttpServer httpServer;

    public static void start(int port) throws IOException {
        if (!isRunning()) {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", httpExchange -> {
                if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    GetRequestScriptEvent.instance.fire(httpExchange);
                }
                //else if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                //    PostRequestScriptEvent.instance.fire(httpExchange);
                //}
            });
            httpServer.setExecutor(Runnable::run);
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
