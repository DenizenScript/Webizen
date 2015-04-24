package space.morphanone.webizen;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import org.bukkit.scheduler.BukkitRunnable;
import space.morphanone.webizen.events.GetRequestScriptEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

public class WebServer {

    private static HttpServer httpServer;

    public static void start(int port) throws IOException {
        if (!isRunning()) {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(final HttpExchange httpExchange) throws IOException {
                    if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        try {
                            httpExchange.getResponseHeaders().set("Content-Type", "text/html");

                            GetRequestScriptEvent event = GetRequestScriptEvent.instance;
                            event.httpExchange = httpExchange;
                            event.fire();
                            int responseCode = event.responseCode != null ? event.responseCode.asInt() : 200;
                            String response = event.response != null ? event.response.asString() : "";
                            byte[] responseBytes = response.getBytes();
                            httpExchange.sendResponseHeaders(responseCode, responseBytes.length);
                            OutputStream out = httpExchange.getResponseBody();
                            out.write(responseBytes);
                            out.close();
                        } catch (IOException e) {
                            dB.echoError(e);
                        }
                    }
                }
            });
            httpServer.setExecutor(new Executor() {
                @Override
                public void execute(final Runnable command) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            command.run();
                        }
                    }.runTask(Webizen.currentInstance);
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
