package space.morphanone.webizen.server;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResponseWrapper {

    private HttpExchange httpExchange;
    private int statusCode = 200;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public ResponseWrapper(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    public void setContentType(String type) {
        this.setHeader("Content-Type", type);
    }

    public void setContentLength(int len) {
        this.setHeader("Content-Length", String.valueOf(len));
    }

    public void setHeader(String name, String value) {
        this.httpExchange.getResponseHeaders().set(name, value);
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public void copyFileFrom(Path path) throws IOException {
        Files.copy(path, buffer);
    }

    public void write(byte[] b) throws IOException {
        buffer.write(b);
    }

    public void send() throws IOException {
        try {
            int bufferSize = buffer.size();
            httpExchange.sendResponseHeaders(statusCode, bufferSize);
            if (bufferSize > 0) {
                OutputStream responseBody = httpExchange.getResponseBody();
                responseBody.write(buffer.toByteArray());
                responseBody.flush();
            }
        } finally {
            httpExchange.close();
        }
    }
}
