package space.morphanone.webizen.events;

public class GetRequestScriptEvent extends BasicRequestScriptEvent {

    // <--[event]
    // @Events
    // get request
    //
    // @Regex ^on get request$
    //
    // @Triggers when the web server receives a GET request
    //
    // @Context
    // <context.address> Returns the IP address of the device that sent the request.
    // <context.request> Returns the path that was requested.
    // <context.query> Returns an ElementTag of the raw query included with the request.
    // <context.query_map> Returns a map of the query. `Potato=Carrot&Corn=Bacon` will return `map@Carrot/el@Carrot|Corn/el@Bacon`.
    // <context.user_info> Returns info about the authenticated user sending the request, if any.
    //
    // @Determine
    // ElementTag to set the content of the response directly
    // "FILE:" + ElementTag to set the file for the response via a file path
    // "PARSED_FILE:" + ElementTag to set the parsed file for the response via a file path, this will parse any denizen tags inside the file
    // "CODE:" + ElementTag to set the HTTP status code of the response (e.g. 200)
    // "TYPE:" + ElementTag to set the MIME (multi purpose mail extension) of the response (e.g. text/html)
    //
    // @Plugin Webizen
    // -->

    public GetRequestScriptEvent() {
        instance = this;
    }

    public static GetRequestScriptEvent instance;

    @Override
    public String getRequestType() {
        return "Get";
    }
}
