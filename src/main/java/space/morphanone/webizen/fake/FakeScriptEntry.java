package space.morphanone.webizen.fake;

import com.denizenscript.denizencore.scripts.ScriptEntry;

public class FakeScriptEntry extends ScriptEntry {

    public FakeScriptEntry() {
        super("FAKE_CMD", null, null);
        setSendingQueue(new FakeScriptQueue());
    }

    @Override
    public FakeScriptQueue getResidingQueue() {
        return (FakeScriptQueue) super.getResidingQueue();
    }

    public static FakeScriptEntry generate() {
        return new FakeScriptEntry();
    }
}
