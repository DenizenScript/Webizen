package space.morphanone.webizen;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import org.bukkit.plugin.java.JavaPlugin;
import space.morphanone.webizen.commands.WebCommand;
import space.morphanone.webizen.events.GetRequestScriptEvent;
import space.morphanone.webizen.events.PostRequestScriptEvent;
import space.morphanone.webizen.server.WebServer;

public class Webizen extends JavaPlugin {

    private static Denizen denizen;
    public static Webizen currentInstance;

    @Override
    public void onEnable() {
        currentInstance = this;
        denizen = (Denizen) getServer().getPluginManager().getPlugin("Denizen");

        if (denizen == null) {
            getLogger().severe("Denizen not found, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DenizenCore.getCommandRegistry().registerCommand(WebCommand.class);
        ScriptEvent.registerScriptEvent(new GetRequestScriptEvent());
        ScriptEvent.registerScriptEvent(new PostRequestScriptEvent());
    }

    @Override
    public void onDisable() {
        WebServer.stop();
        currentInstance = null;
        denizen = null;
    }
}
