package space.morphanone.webizen.server;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;

public class RequestWrapper {

    private HttpExchange httpExchange;
    private byte[] entireRequest;
    private String fileName = null;

    public RequestWrapper(HttpExchange httpExchange) throws IOException {
        this.httpExchange = httpExchange;
        InputStream requestBody = httpExchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[0xFFFF];
        int b;
        while ((b = requestBody.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, b);
        }
        this.entireRequest = buffer.toByteArray();
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public long getContentLengthLong() {
        try {
            return Long.parseLong(getHeader("Content-Length"));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getHeader(String name) {
        return httpExchange.getRequestHeaders().getFirst(name);
    }

    public String getMethod() {
        return httpExchange.getRequestMethod();
    }

    public String getRemoteAddr() {
        return httpExchange.getRemoteAddress().toString();
    }

    public URI getReqURI() {
        return httpExchange.getRequestURI();
    }

    public byte[] getFile() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(entireRequest);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        byte[] boundary = reader.readLine().getBytes("UTF-8");
        String nextLine;
        int max = entireRequest.length - boundary.length - 2;
        while (true) {
            nextLine = reader.readLine();
            if (this.fileName == null) {
                int start = nextLine.indexOf("; name=\"", nextLine.indexOf(";"));
                int end = nextLine.indexOf("\"", start + 9);
                int startOffset = 8;
                if (start == -1 || end == -1) {
                    start = nextLine.indexOf("; name=", end);
                    end = nextLine.indexOf(";", start + 8);
                    startOffset = 7;
                }
                this.fileName = nextLine.substring(start + startOffset, end);
            }
            max -= nextLine.getBytes("UTF-8").length;
            if (nextLine.startsWith(" ") || nextLine.startsWith("\t")) {
                break;
            }
        }
        byte[] buffer = new byte[max];
        int b;
        int read = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while ((b = inputStream.read(buffer, 0, buffer.length)) != -1 && read < max) {
            outputStream.write(buffer, 0, b);
            read += b;
        }
        reader.close();
        return outputStream.toByteArray();
    }

    public String getFileName() {
        return this.fileName;
    }
}
