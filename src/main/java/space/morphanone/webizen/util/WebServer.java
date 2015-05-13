package space.morphanone.webizen.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.aufdemrand.denizen.tags.BukkitTagContext;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import org.bukkit.scheduler.BukkitRunnable;
import space.morphanone.webizen.Webizen;
import space.morphanone.webizen.events.GetRequestScriptEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServer {

    private static final Pattern tagPattern = Pattern.compile("<\\{([^\\}]*)\\}>");
    private static final FakeScriptEntry reusableScriptEntry = FakeScriptEntry.generate();
    private static final BukkitTagContext reusableTagContext = new BukkitTagContext(reusableScriptEntry, false);

    private static HttpServer httpServer;

    public static void start(int port) throws IOException {
        if (!isRunning()) {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(final HttpExchange httpExchange) throws IOException {
                    if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        try {
                            // Fire the GetRequestScriptEvent for this request
                            GetRequestScriptEvent event = GetRequestScriptEvent.instance;
                            event.httpExchange = httpExchange;
                            event.contentType = null;
                            event.response = null;
                            event.responseCode = null;
                            event.responseFile = null;
                            event.parseFile = null;
                            event.fire();

                            // Add context to fake ScriptQueue for parsed file tags
                            reusableScriptEntry.getResidingQueue().setContext(event.getContext());

                            // Set HTTP response headers before anything else
                            String contentType = event.contentType != null ? event.contentType.asString() : "text/html";
                            httpExchange.getResponseHeaders().set("Content-Type", contentType);
                            int responseCode = event.responseCode != null ? event.responseCode.asInt() : 200;

                            // Handle the determinations from the event and send in the response body
                            OutputStream out = httpExchange.getResponseBody();
                            if (event.response == null && event.responseFile == null) {
                                httpExchange.sendResponseHeaders(responseCode, 0);
                            }
                            else if (event.response != null) {
                                byte[] responseBytes = event.response.asString().getBytes("UTF-8");
                                httpExchange.sendResponseHeaders(responseCode, responseBytes.length);
                                out.write(responseBytes);
                            }
                            else if (event.parseFile.asBoolean()) {
                                String html = new String(Files.readAllBytes(event.responseFile.toPath()));
                                Matcher m = tagPattern.matcher(html);
                                StringBuffer s = new StringBuffer(html.length());
                                while (m.find()) {
                                    String parsed = TagManager.readSingleTag(m.group(1), reusableTagContext);
                                    // If the parsed output is null, allow Denizen to handle the debugging
                                    // and return "null"
                                    if (parsed != null) {
                                        String cleaned = TagManager.cleanOutput(parsed);
                                        m.appendReplacement(s, Matcher.quoteReplacement(cleaned));
                                    }
                                    else {
                                        m.appendReplacement(s, "null");
                                    }
                                }
                                m.appendTail(s);
                                byte[] responseBytes = s.toString().getBytes("UTF-8");
                                httpExchange.sendResponseHeaders(responseCode, responseBytes.length);
                                out.write(responseBytes);
                            }
                            else {
                                httpExchange.sendResponseHeaders(responseCode, event.responseFile.length());
                                Files.copy(event.responseFile.toPath(), out);
                            }
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
