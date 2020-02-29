package space.morphanone.webizen.fake;

import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

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
}
