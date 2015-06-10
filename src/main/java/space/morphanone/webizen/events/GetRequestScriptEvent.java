package space.morphanone.webizen.events;

public class GetRequestScriptEvent extends BasicRequestScriptEvent {

    public GetRequestScriptEvent() {
        instance = this;
    }

    public static GetRequestScriptEvent instance;

    @Override
    public String getRequestType() {
        return "Get";
    }
}
