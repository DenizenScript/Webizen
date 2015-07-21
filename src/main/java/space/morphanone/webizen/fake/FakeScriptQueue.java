package space.morphanone.webizen.fake;

import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;

import java.util.HashMap;

public class FakeScriptQueue extends ScriptQueue {

    protected FakeScriptQueue() {
        super("FAKE_WEBIZEN_QUEUE");
    }

    @Override
    protected void onStart() {
        throw new IllegalStateException("This is a fake queue!");
    }

    @Override
    protected void onStop() {
        throw new IllegalStateException("This is a fake queue!");
    }

    @Override
    protected boolean shouldRevolve() {
        throw new IllegalStateException("This is a fake queue!");
    }

    public void setContext(HashMap<String, dObject> context) {
        this.cachedContext = context;
    }
}
