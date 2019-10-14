package xyz.gianlu.librespot.debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.spotify.connectstate.model.Connect;
import xyz.gianlu.librespot.common.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class PutStateAnalyzer {

    public static void main(String[] args) throws FileNotFoundException {
        JsonObject data = (JsonObject) new JsonParser().parse(new FileReader(new File(args[0])));
        System.out.println("Loaded data! Entries: " + data.size());

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");

            if (!scanner.hasNextLine()) return;
            String cmd = scanner.nextLine();
            String[] split = cmd.split("\\s");
            command(data, split[0], Arrays.copyOfRange(split, 1, split.length));
        }
    }

    private static void command(JsonObject data, String cmd, String[] args) {
        switch (cmd) {
            case "dump":
                if (args.length < 1) {
                    System.out.println("Missing sub command!");
                    return;
                }

                dump(data, args[0], Arrays.copyOfRange(args, 1, args.length));
                return;
            case "compare":
                if (args.length < 3) {
                    System.out.println("Missing compare codes or subcommand!");
                    return;
                }

                compare(data, args[0], args[1], args[2]);
                return;
            default:
                System.out.println("Unknown command!");
                return;
        }
    }

    private static void compare(JsonObject data, String one, String other, String subcmd) {
        JsonObject oneObj = data.getAsJsonObject(one);
        JsonObject otherObj = data.getAsJsonObject(other);
        if (oneObj == null || otherObj == null) return;

        switch (subcmd) {
            case "req":
                try {
                    compareProto(Connect.PutStateRequest.parseFrom(Utils.hexToBytes(oneObj.get("req").getAsString())),
                            Connect.PutStateRequest.parseFrom(Utils.hexToBytes(otherObj.get("req").getAsString())));
                } catch (InvalidProtocolBufferException ex) {
                    throw new IllegalStateException(ex);
                }
                return;
            case "resp":
                try {
                    compareProto(Connect.Cluster.parseFrom(Utils.hexToBytes(oneObj.get("resp").getAsString())),
                            Connect.Cluster.parseFrom(Utils.hexToBytes(otherObj.get("resp").getAsString())));
                } catch (InvalidProtocolBufferException ex) {
                    throw new IllegalStateException(ex);
                }
                return;
            default:
                return;
        }
    }

    private static void compareProto(AbstractMessage one, AbstractMessage other) {
        if (one.getClass() != other.getClass()) throw new IllegalArgumentException();

        for (Descriptors.FieldDescriptor field : one.getDescriptorForType().getFields()) {
            Object oneVal = one.getField(field);
            Object otherVal = other.getField(field);
            if (oneVal instanceof AbstractMessage && otherVal instanceof AbstractMessage)
                compareProto((AbstractMessage) oneVal, (AbstractMessage) otherVal);
            else if (!oneVal.equals(otherVal)) {
                if (oneVal instanceof List) oneVal = "array[" + ((List) oneVal).size() + "]";
                if (otherVal instanceof List) otherVal = "array[" + ((List) otherVal).size() + "]";

                System.out.println(field + ": " + oneVal + " != " + otherVal);
            }
        }
    }

    private static void dump(JsonObject data, String subcmd, String[] args) {
        if (args.length < 1) {
            System.out.println("Missing entry code!");
            return;
        }

        JsonObject entry = data.getAsJsonObject(args[0]);
        switch (subcmd) {
            case "json":
                System.out.println(entry);
                return;
            case "callTrace":
                System.out.println(entry.get("callTrace").getAsString());
                return;
            case "details":
                System.out.println(String.format("Sent: %d, Process time: %d, HTTP code: %d",
                        entry.get("sent").getAsLong(), entry.get("processTime").getAsLong(),
                        entry.get("statusCode").getAsInt()));
                return;
            default:
                return;
        }
    }
}
