package xyz.gianlu.librespot.debug;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Message;
import com.spotify.connectstate.model.Connect;
import okhttp3.Response;
import xyz.gianlu.librespot.common.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class PutStateDebugger {
    private static final AtomicInteger requestCounter = new AtomicInteger();
    private static final Map<Integer, Entry> entries = new HashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(PutStateDebugger::dump));
    }

    private static void dump() {
        JsonObject obj = new JsonObject();
        for (int code : entries.keySet())
            obj.add(String.valueOf(code), entries.get(code).toJson());

        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setLenient(true);
            jsonWriter.setIndent("");
            Streams.write(obj, jsonWriter);
            System.out.println("============= PUT STATE REQUEST OUTPUT START =============");
            System.out.println(stringWriter.toString());
            System.out.println("============= PUT STATE REQUEST OUTPUT END =============");

            File logFile = new File("put-state-debug-" + System.currentTimeMillis() + ".json");
            try (FileOutputStream out = new FileOutputStream(logFile)) {
                out.write(stringWriter.toString().getBytes());
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static int registerRequest(Connect.PutStateRequest proto) {
        int code = requestCounter.getAndIncrement();
        entries.put(code, Entry.forRequest(proto));
        System.out.println("============ PUT STATE REQUEST #" + code + " ============");
        return code;
    }

    public static void registerResponse(int code, Response resp) {
        Entry entry = entries.get(code);
        if (entry == null) throw new IllegalArgumentException(String.valueOf(code));

        entry.setResponse(resp);
    }

    private static class Entry {
        private final long sent;
        private final String callTrace;
        private final Connect.PutStateRequest req;
        private int processTime = -1;
        private int statusCode = 0;
        private Connect.Cluster resp;

        private Entry(long sent, String callTrace, Connect.PutStateRequest req) {
            this.sent = sent;
            this.callTrace = callTrace;
            this.req = req;
        }

        static Entry forRequest(Connect.PutStateRequest proto) {
            long ts = System.currentTimeMillis();

            String stackTrace;
            try {
                throw new Exception();
            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                stackTrace = sw.toString();
            }

            return new Entry(ts, stackTrace, proto);
        }

        private static String protoToHex(Message msg) {
            if (msg == null) return "";

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                msg.writeTo(out);
                return Utils.bytesToHex(out.toByteArray());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void setResponse(Response http) {
            processTime = (int) (System.currentTimeMillis() - sent);
            statusCode = http.code();

            try {
                this.resp = Connect.Cluster.parseFrom(http.body().byteStream());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("sent", sent);
            obj.addProperty("callTrace", callTrace);
            obj.addProperty("processTime", processTime);
            obj.addProperty("statusCode", statusCode);
            obj.addProperty("req", protoToHex(req));
            obj.addProperty("resp", protoToHex(resp));
            return obj;
        }
    }
}
