package space.morphanone.webizen.events;

import com.sun.net.httpserver.HttpExchange;
import net.aufdemrand.denizen.BukkitScriptEntryData;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.HashMap;

public class GetRequestScriptEvent extends ScriptEvent {

    public GetRequestScriptEvent() {
        instance = this;
    }

    public static GetRequestScriptEvent instance;
    public HttpExchange httpExchange;
    public Element response;
    public Element responseCode;

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return lower.startsWith("get ");
    }

    @Override
    public boolean matches(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return CoreUtilities.xthArgEquals(2, lower, "request");
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
        context.put("request", new Element(httpExchange.getRequestURI().toString()));
        return context;
    }
}
