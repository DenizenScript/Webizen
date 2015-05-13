package space.morphanone.webizen.util;

import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;

import java.util.Map;

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

    public void setContext(Map<String, dObject> context) {
        for (Map.Entry<String, dObject> entry : context.entrySet()) {
            this.addContext(entry.getKey(), entry.getValue());
        }
    }
}
