package space.morphanone.webizen.events;

import com.sun.net.httpserver.HttpExchange;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizen.utilities.Utilities;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import space.morphanone.webizen.server.RequestWrapper;
import space.morphanone.webizen.server.ResponseWrapper;

import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostRequestScriptEvent extends BasicRequestScriptEvent {

    // <--[event]
    // @Events
    // post request
    //
    // @Regex ^on post request$
    //
    // @Cancellable false
    //
    // @Triggers when the web server receives a GET request
    //
    // @Context
    // <context.address> Returns the IP address of the device that sent the request.
    // <context.request> Returns the path that was requested
    // <context.query> Returns a dList of the query included with the request
    // <context.user_info> Returns info about the authenticated user sending the request, if any.
    // <context.upload_name> returns the name of the file posted.
    // <context.upload_size_mb> returns the size of the upload in MegaBytes (where 1 MegaByte = 1 000 000 Bytes).
    //
    // @Determine
    // Element to set the content of the response directly
    // "FILE:" + Element to set the file for the response via a file path
    // "PARSED_FILE:" + Element to set the parsed file for the response via a file path, this will parse any denizen tags inside the file
    // "CODE:" + Element to set the HTTP status code of the response (e.g. 200)
    // "TYPE:" + Element to set the MIME (multi purpose mail extension) of the response (e.g. text/html)
    // "SAVE_UPLOAD:" + Element to save the upload to a file.
    //
    // @Plugin Webizen
    // -->
    public PostRequestScriptEvent() {
        instance = this;
    }

    public static PostRequestScriptEvent instance;
    public Element saveUpload;
    public byte[] requestBody;
    public Element fileName;

    @Override
    public String getRequestType() {
        return "Post";
    }

    @Override
    public void fire(HttpExchange httpExchange) {
        try {
            RequestWrapper request = new RequestWrapper(httpExchange);
            this.requestBody = request.getFile();
            this.fileName = new Element(request.getFileName());
        } catch (Exception e) {
            dB.echoError(e);
        }
        this.saveUpload = null;

        super.fire(httpExchange);

        if (this.saveUpload != null) {
            try {
                File file = new File(DenizenAPI.getCurrentInstance().getDataFolder(), saveUpload.asString());
                if (!Utilities.isSafeFile(file)) {
                    dB.echoError("Save failed: cannot save there!");
                    return;
                }
                file.getParentFile().mkdirs();
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(this.requestBody);
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean applyDetermination(ScriptContainer container, String determination) {
        String lower = CoreUtilities.toLowerCase(determination);
        if (lower.startsWith("save_upload:")) {
            saveUpload = new Element(determination.substring(12));
            return true;
        }
        return super.applyDetermination(container, determination);
    }

    @Override
    public HashMap<String, dObject> getContext() {
        HashMap<String, dObject> context = super.getContext();
        context.put("upload_name", fileName);
        context.put("upload_size_mb", new Element(requestBody.length/(1000*1000)));
        return context;
    }
}
