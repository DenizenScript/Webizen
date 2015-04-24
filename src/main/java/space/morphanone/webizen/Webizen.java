package space.morphanone.webizen;

import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizencore.events.ScriptEvent;
import org.bukkit.plugin.java.JavaPlugin;
import space.morphanone.webizen.commands.WebCommand;
import space.morphanone.webizen.events.GetRequestScriptEvent;

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
        }

        new WebCommand().activate().as("WEB").withOptions("web [start/stop] (port:<#>{80})", 1);
        ScriptEvent.registerScriptEvent(new GetRequestScriptEvent());
    }

    @Override
    public void onDisable() {
        WebServer.stop();
        currentInstance = null;
        denizen = null;
    }
}
