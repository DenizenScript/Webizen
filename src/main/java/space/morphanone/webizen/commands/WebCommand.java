package space.morphanone.webizen.commands;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import space.morphanone.webizen.server.WebServer;

import java.io.IOException;
import java.net.BindException;

public class WebCommand extends AbstractCommand {

    // <--[command]
    // @Name Web
    // @Syntax Web [start/stop] (port:<#>)
    // @Required 1
    // @Stable unstable
    // @Short Manages the web server ran on your minecraft server
    // @Author Morphan1
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

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (arg.matchesEnum(Action.values())
                    && !scriptEntry.hasObject("action")) {
                scriptEntry.addObject("action", arg.asElement());
            }

            else if (arg.matchesPrefix("port")
                    && arg.matchesPrimitive(aH.PrimitiveType.Integer)
                    && !scriptEntry.hasObject("port")) {
                scriptEntry.addObject("port", arg.asElement());
            }

            else arg.reportUnhandled();
        }

        if (!scriptEntry.hasObject("action"))
            throw new InvalidArgumentsException("Must specify an action!");

        scriptEntry.defaultObject("port", new Element(80));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element actionElement = scriptEntry.getElement("action");
        Element portElement = scriptEntry.getElement("port");

        dB.report(scriptEntry, getName(), actionElement.debug() + portElement.debug());

        switch (Action.valueOf(actionElement.asString().toUpperCase())) {
            case START:
                if (WebServer.isRunning()) {
                    dB.log("A Webizen server is already running!");
                    return;
                }
                try {
                    WebServer.start(portElement.asInt());
                } catch (BindException e) {
                    dB.echoError("Could not bind a Webizen server to port " + portElement.asInt() + ". Perhaps there" +
                            " is already something using that port?");
                } catch (IOException e) {
                    dB.echoError("There was a problem while starting a Webizen server...");
                    dB.echoError(e);
                }
                break;
            case STOP:
                if (!WebServer.isRunning()) {
                    dB.log("There is no Webizen server currently running!");
                    return;
                }
                WebServer.stop();
                break;
        }
    }
}
