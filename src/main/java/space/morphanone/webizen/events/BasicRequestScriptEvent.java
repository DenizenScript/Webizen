package space.morphanone.webizen.events;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.sun.net.httpserver.HttpExchange;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizen.utilities.DenizenAPI;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import space.morphanone.webizen.fake.FakeScriptEntry;
import space.morphanone.webizen.server.ResponseWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicRequestScriptEvent extends ScriptEvent {

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

        fire();

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
                        String parsed = TagManager.readSingleTag(m.group(1), reusableTagContext);
                        // If the parsed output is null, allow Denizen to handle the debugging
                        // and return "null"\
                        m.appendReplacement(s, parsed != null ? Matcher.quoteReplacement(parsed) : "null");
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
        return path.eventLower.startsWith(lowerRequestType + " ");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return path.eventArgLowerAt(1).equals("request");
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
            File file = new File(DenizenAPI.getCurrentInstance().getDataFolder(), determinationObj.toString().substring(5));
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
            File file = new File(DenizenAPI.getCurrentInstance().getDataFolder(), determinationObj.toString().substring(12));
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
        if (name.equals("address")) {
            return new ElementTag(httpExchange.getRemoteAddress().toString());
        }
        else if (name.equals("query")) {
            String query = httpExchange.getRequestURI().getQuery();
            return new ElementTag(query != null ? query : "");
        }
        else if (name.equals("query_map")) {
            MapTag mappedValues = new MapTag();
            String query = httpExchange.getRequestURI().getQuery();
            if (query != null) {
                for (String value : CoreUtilities.split(query, '&')) {
                    List<String> split = CoreUtilities.split(value, '=');
                    mappedValues.map.put(new StringHolder(split.get(0)), new ElementTag(split.get(1)));
                }
            }
            return mappedValues;
        }
        else if (name.equals("request")) {
            return new ElementTag(httpExchange.getRequestURI().getPath());
        }
        else if (name.equals("user_info")) {
            return new ElementTag(httpExchange.getRequestURI().getUserInfo());
        }
        return super.getContext(name);
    }
}
