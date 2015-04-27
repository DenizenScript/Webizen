package space.morphanone.webizen.events;

import com.sun.net.httpserver.HttpExchange;
import net.aufdemrand.denizen.BukkitScriptEntryData;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

public class GetRequestScriptEvent extends ScriptEvent {

    public GetRequestScriptEvent() {
        instance = this;
    }

    public static GetRequestScriptEvent instance;
    public HttpExchange httpExchange;
    public Element contentType;
    public Element response;
    public Element responseCode;
    public File responseFile;

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return lower.startsWith("get ");
    }

    @Override
    public boolean matches(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return CoreUtilities.xthArgEquals(1, lower, "request");
    }

    @Override
    public String getName() {
        return "GetRequest";
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
        }
        else if (lower.startsWith("type:")) {
            contentType = new Element(lower.substring(5));
        }
        else {
            response = new Element(determination);
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
