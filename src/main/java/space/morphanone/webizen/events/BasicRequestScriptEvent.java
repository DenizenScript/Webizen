package space.morphanone.webizen.events;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.sun.net.httpserver.HttpExchange;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.scheduler.BukkitRunnable;
import space.morphanone.webizen.fake.FakeScriptEntry;
import space.morphanone.webizen.server.ResponseWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicRequestScriptEvent extends BukkitScriptEvent {

    public HttpExchange httpExchange;

    public class ResponseOptions {
        public ElementTag contentType;
        public ElementTag responseText;
        public ElementTag responseCode;
        public File responseFile;
        public ElementTag parseFile;
    }

    public ResponseOptions scriptResponse;

    private String requestType = getRequestType();
    private String lowerRequestType = CoreUtilities.toLowerCase(requestType);

    private static final Pattern tagPattern = Pattern.compile("<\\{([^\\}]*)\\}>");
    private static final CharsetDecoder utfDecoder = StandardCharsets.UTF_8.newDecoder();
    private static final FakeScriptEntry reusableScriptEntry = FakeScriptEntry.generate();
    private static final BukkitTagContext reusableTagContext = new BukkitTagContext(reusableScriptEntry);

    public abstract String getRequestType();

    public void fire(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        scriptResponse = new ResponseOptions();

        CompletableFuture future = new CompletableFuture();
        BukkitScriptEvent altEvent = (BukkitScriptEvent) clone();
        new BukkitRunnable() {
            @Override
            public void run() {
                altEvent.fire();
                future.complete(null);
            }
        }.runTask(Denizen.getInstance());

        try {
            future.get();
        }
        catch (InterruptedException | ExecutionException ex) {
            Debug.echoError(ex);
        }

        ResponseWrapper response = new ResponseWrapper(httpExchange);
        try {
            // Add context to fake ScriptQueue for parsed file tags
            reusableScriptEntry.getResidingQueue().setContextSource(this);

            // Set HTTP response headers before anything else
            response.setContentType(scriptResponse.contentType != null ? scriptResponse.contentType.asString() : "text/html");
            if (scriptResponse.responseCode != null) {
                response.setStatus(scriptResponse.responseCode.asInt());
            }

            if (scriptResponse.responseText != null || scriptResponse.responseFile != null) {
                if (scriptResponse.responseText != null) {
                    response.write(scriptResponse.responseText.asString().getBytes(StandardCharsets.UTF_8));
                }
                else if (scriptResponse.parseFile.asBoolean()) {
                    FileInputStream input = new FileInputStream(scriptResponse.responseFile);
                    FileChannel channel = input.getChannel();
                    ByteBuffer bbuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                    StringBuffer s = new StringBuffer();
                    Matcher m = tagPattern.matcher(utfDecoder.decode(bbuf));
                    while (m.find()) {
                        ObjectTag parsed = TagManager.parseTextToTag(m.group(1), reusableTagContext).parse(reusableTagContext);
                        // If the parsed output is null, allow Denizen to handle the debugging
                        // and return "null"
                        m.appendReplacement(s, parsed != null ? Matcher.quoteReplacement(parsed.toString()) : "null");
                    }
                    m.appendTail(s);
                    response.write(s.toString().getBytes(StandardCharsets.UTF_8));
                }
                else {
                    response.copyFileFrom(scriptResponse.responseFile.toPath());
                }
            }
        } catch (IOException e) {
            Debug.echoError(e);
        }
        try {
            response.send();
        } catch (IOException e) {
            Debug.echoError(e);
        }
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith(lowerRequestType + " request")) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return requestType + "Request";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String lower = CoreUtilities.toLowerCase(determinationObj.toString());
        if (lower.startsWith("code:")) {
            ElementTag code = new ElementTag(lower.substring(5));
            if (!code.isInt()) {
                Debug.echoError("Determination for 'code' must be a valid number.");
                return false;
            }
            scriptResponse.responseCode = code;
        }
        else if (lower.startsWith("file:")) {
            File file = new File(Denizen.getInstance().getDataFolder(), determinationObj.toString().substring(5));
            if (!file.exists()) {
                Debug.echoError("File '" + file + "' does not exist.");
                return false;
            }
            if (!Utilities.canReadFile(file)) {
                Debug.echoError("File '" + file + "' is restricted from access by the Denizen config.");
                return false;
            }
            scriptResponse.responseFile = file;
            scriptResponse.parseFile = new ElementTag("false");
        }
        else if (lower.startsWith("parsed_file:")) {
            File file = new File(Denizen.getInstance().getDataFolder(), determinationObj.toString().substring(12));
            if (!file.exists()) {
                Debug.echoError("File '" + file + "' does not exist.");
                return false;
            }
            if (!Utilities.canReadFile(file)) {
                Debug.echoError("File '" + file + "' is restricted from access by the Denizen config.");
                return false;
            }
            scriptResponse.responseFile = file;
            scriptResponse.parseFile = new ElementTag("true");
        }
        else if (lower.startsWith("type:")) {
            scriptResponse.contentType = new ElementTag(lower.substring(5));
        }
        else {
            scriptResponse.responseText = new ElementTag(determinationObj.toString());
        }
        return true;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(null, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "address":
                return new ElementTag(httpExchange.getRemoteAddress().toString());
            case "query": {
                String query = httpExchange.getRequestURI().getQuery();
                return new ElementTag(query != null ? query : "");
            }
            case "query_map": {
                MapTag mappedValues = new MapTag();
                String query = httpExchange.getRequestURI().getQuery();
                if (query != null) {
                    for (String value : CoreUtilities.split(query, '&')) {
                        List<String> split = CoreUtilities.split(value, '=', 2);
                        try {
                            String split_key = java.net.URLDecoder.decode(split.get(0), "UTF-8");
                            String split_value = java.net.URLDecoder.decode(split.get(1), "UTF-8");
                            mappedValues.putObject(split_key, new ElementTag(split_value));
                        }
                        catch (UnsupportedEncodingException e) {
                            Debug.echoError(e);
                        }
                    }
                }
                return mappedValues;
            }
            case "request":
                return new ElementTag(httpExchange.getRequestURI().getPath());
            case "user_info":
                return new ElementTag(httpExchange.getRequestURI().getUserInfo());
        }
        return super.getContext(name);
    }
}
