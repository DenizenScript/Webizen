package space.morphanone.webizen.events;

import com.sun.net.httpserver.HttpExchange;
import com.denizenscript.denizen.utilities.DenizenAPI;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import space.morphanone.webizen.server.RequestWrapper;

import java.io.*;
import java.util.HashMap;

public class PostRequestScriptEvent extends BasicRequestScriptEvent {

    // <--[event]
    // @Events
    // post request
    //
    // @Regex ^on post request$
    //
    // @Triggers when the web server receives a POST request
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
    // ElementTag to set the content of the response directly
    // "FILE:" + ElementTag to set the file for the response via a file path
    // "PARSED_FILE:" + ElementTag to set the parsed file for the response via a file path, this will parse any denizen tags inside the file
    // "CODE:" + ElementTag to set the HTTP status code of the response (e.g. 200)
    // "TYPE:" + ElementTag to set the MIME (multi purpose mail extension) of the response (e.g. text/html)
    // "SAVE_UPLOAD:" + ElementTag to save the upload to a file.
    //
    // @Plugin Webizen
    // -->
    public PostRequestScriptEvent() {
        instance = this;
    }

    public static PostRequestScriptEvent instance;
    public ElementTag saveUpload;
    public byte[] requestBody;
    public ElementTag fileName;

    @Override
    public String getRequestType() {
        return "Post";
    }

    @Override
    public void fire(HttpExchange httpExchange) {
        try {
            RequestWrapper request = new RequestWrapper(httpExchange);
            this.requestBody = request.getFile();
            this.fileName = new ElementTag(request.getFileName());
        } catch (Exception e) {
            Debug.echoError(e);
        }
        this.saveUpload = null;

        super.fire(httpExchange);

        if (this.saveUpload != null) {
            try {
                File file = new File(DenizenAPI.getCurrentInstance().getDataFolder(), saveUpload.asString());
                if (!Utilities.canWriteToFile(file)) {
                    Debug.echoError("Save failed: cannot save there!");
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
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String lower = CoreUtilities.toLowerCase(determinationObj.toString());
        if (lower.startsWith("save_upload:")) {
            saveUpload = new ElementTag(determinationObj.toString().substring(12));
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public HashMap<String, ObjectTag> getContext() {
        HashMap<String, ObjectTag> context = super.getContext();
        context.put("upload_name", fileName);
        context.put("upload_size_mb", new ElementTag(requestBody.length/(1000*1000)));
        return context;
    }
}
