package space.morphanone.webizen.util;

import net.aufdemrand.denizencore.exceptions.ScriptEntryCreationException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class FakeScriptEntry extends ScriptEntry {

    public FakeScriptEntry() throws ScriptEntryCreationException {
        super("FAKE_CMD", null, null);
        setSendingQueue(new FakeScriptQueue());
    }

    @Override
    public FakeScriptQueue getResidingQueue() {
        return (FakeScriptQueue) super.getResidingQueue();
    }

    public static FakeScriptEntry generate() {
        try {
            return new FakeScriptEntry();
        } catch (ScriptEntryCreationException e) {
            dB.echoError(e);
            return null;
        }
    }
}
