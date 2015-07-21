package space.morphanone.webizen.events;

import com.sun.net.httpserver.HttpExchange;
import net.aufdemrand.denizen.BukkitScriptEntryData;
import net.aufdemrand.denizen.tags.BukkitTagContext;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import space.morphanone.webizen.fake.FakeScriptEntry;
import space.morphanone.webizen.server.ResponseWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicRequestScriptEvent extends ScriptEvent {

    public HttpExchange httpExchange;
    public Element contentType;
    public Element responseText;
    public Element responseCode;
    public File responseFile;
    public Element parseFile;

    private String requestType = getRequestType();
    private String lowerRequestType = CoreUtilities.toLowerCase(requestType);

    private static final Pattern tagPattern = Pattern.compile("<\\{([^\\}]*)\\}>");
    private static final CharsetDecoder utfDecoder = Charset.forName("UTF-8").newDecoder();
    private static final FakeScriptEntry reusableScriptEntry = FakeScriptEntry.generate();
    private static final BukkitTagContext reusableTagContext = new BukkitTagContext(reusableScriptEntry, false);

    public abstract String getRequestType();

    public void fire(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        this.contentType = null;
        this.responseText = null;
        this.responseCode = null;
        this.responseFile = null;
        this.parseFile = null;

        fire();

        ResponseWrapper response = new ResponseWrapper(httpExchange);
        try {
            // Add context to fake ScriptQueue for parsed file tags
            reusableScriptEntry.getResidingQueue().setContext(getContext());

            // Set HTTP response headers before anything else
            response.setContentType(this.contentType != null ? this.contentType.asString() : "text/html");
            if (this.responseCode != null) {
                response.setStatus(this.responseCode.asInt());
            }

            if (this.responseText != null || this.responseFile != null) {
                if (this.responseText != null) {
                    response.write(this.responseText.asString().getBytes("UTF-8"));
                }
                else if (this.parseFile.asBoolean()) {
                    FileInputStream input = new FileInputStream(this.responseFile);
                    FileChannel channel = input.getChannel();
                    ByteBuffer bbuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                    StringBuffer s = new StringBuffer();
                    Matcher m = tagPattern.matcher(utfDecoder.decode(bbuf));
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
                    response.write(s.toString().getBytes("UTF-8"));
                }
                else {
                    response.copyFileFrom(this.responseFile.toPath());
                }
            }
        } catch (IOException e) {
            dB.echoError(e);
        }
        try {
            response.send();
        } catch (IOException e) {
            dB.echoError(e);
        }
    }

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return lower.startsWith(lowerRequestType + " ");
    }

    @Override
    public boolean matches(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return CoreUtilities.xthArgEquals(1, lower, "request");
    }

    @Override
    public String getName() {
        return requestType + "Request";
    }

    @Override
    public boolean applyDetermination(ScriptContainer container, String determination) {
        String lower = CoreUtilities.toLowerCase(determination);
        if (lower.startsWith("code:")) {
            Element code = new Element(lower.substring(5));
            if (!code.isInt()) {
                dB.echoError("Determination for 'code' must be a valid number.");
                return false;
            }
            responseCode = code;
        }
        else if (lower.startsWith("file:")) {
            File file = new File(DenizenAPI.getCurrentInstance().getDataFolder(), lower.substring(5));
            if (!file.exists()) {
                dB.echoError("File '" + file + "' does not exist.");
                return false;
            }
            responseFile = file;
            parseFile = Element.FALSE;
        }
        else if (lower.startsWith("parsed_file:")) {
            File file = new File(DenizenAPI.getCurrentInstance().getDataFolder(), lower.substring(12));
            if (!file.exists()) {
                dB.echoError("File '" + file + "' does not exist.");
                return false;
            }
            responseFile = file;
            parseFile = Element.TRUE;
        }
        else if (lower.startsWith("type:")) {
            contentType = new Element(lower.substring(5));
        }
        else {
            responseText = new Element(determination);
        }
        return true;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(null, null);
    }

    @Override
    public HashMap<String, dObject> getContext() {
        HashMap<String, dObject> context = super.getContext();
        context.put("address", new Element(httpExchange.getRemoteAddress().toString()));
        URI uri = httpExchange.getRequestURI();
        context.put("query", new Element(uri.getQuery()));
        context.put("request", new Element(uri.getPath()));
        context.put("user_info", new Element(uri.getUserInfo()));
        return context;
    }
}
