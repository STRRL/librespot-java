package xyz.gianlu.librespot.debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
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
            default:
                System.out.println("Unknown command!");
                return;
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
        }
    }
}
