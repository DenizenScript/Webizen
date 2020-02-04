package space.morphanone.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import space.morphanone.webizen.server.WebServer;

import java.io.IOException;
import java.net.BindException;

public class WebCommand extends AbstractCommand {

    // <--[command]
    // @Name Web
    // @Syntax Web [start/stop] (port:<#>)
    // @Required 1
    // @Short Manages the web server ran on your minecraft server
    // @group addons
    //
    // @Description
    // Lets you start or stop a web server
    // Only one web server can run on the server at once
    // The default port when the port argument is not specified, is 80
    // TODO: Document command details!
    //
    // @Usage
    // Use to start the web server on port 10123
    // - web start port:10123
    //
    // @Usage
    // Use to stop the web server
    // - web stop
    //
    // @Plugin Webizen
    // -->

    private enum Action { START, STOP }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (arg.matchesEnum(Action.values())
                    && !scriptEntry.hasObject("action")) {
                scriptEntry.addObject("action", arg.asElement());
            }

            else if (arg.matchesPrefix("port")
                    && arg.matchesPrimitive(ArgumentHelper.PrimitiveType.Integer)
                    && !scriptEntry.hasObject("port")) {
                scriptEntry.addObject("port", arg.asElement());
            }

            else arg.reportUnhandled();
        }

        if (!scriptEntry.hasObject("action"))
            throw new InvalidArgumentsException("Must specify an action!");

        scriptEntry.defaultObject("port", new ElementTag(80));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        ElementTag actionElementTag = scriptEntry.getElement("action");
        ElementTag portElementTag = scriptEntry.getElement("port");

        Debug.report(scriptEntry, getName(), actionElementTag.debug() + portElementTag.debug());

        switch (Action.valueOf(actionElementTag.asString().toUpperCase())) {
            case START:
                if (WebServer.isRunning()) {
                    Debug.log("A Webizen server is already running!");
                    return;
                }
                try {
                    WebServer.start(portElementTag.asInt());
                } catch (BindException e) {
                    Debug.echoError("Could not bind a Webizen server to port " + portElementTag.asInt() + ". Perhaps there" +
                            " is already something using that port?");
                } catch (IOException e) {
                    Debug.echoError("There was a problem while starting a Webizen server...");
                    Debug.echoError(e);
                }
                break;
            case STOP:
                if (!WebServer.isRunning()) {
                    Debug.log("There is no Webizen server currently running!");
                    return;
                }
                WebServer.stop();
                break;
        }
    }
}
