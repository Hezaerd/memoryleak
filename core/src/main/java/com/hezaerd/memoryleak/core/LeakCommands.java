package com.hezaerd.memoryleak.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class LeakCommands {
    public static final String NAME = "leak";
    public static final String PERMISSION = "memoryleak.admin";
    public static final String USAGE = "/leak <start <MB> <seconds>|stop|clear|status>";
    public static final List<String> SUBCOMMANDS =
            Collections.unmodifiableList(Arrays.asList("start", "stop", "clear", "status"));

    private LeakCommands() {
    }

    public static Reply start(LeakManager leak, int megabytes, int seconds) {
        try {
            leak.start(megabytes, seconds);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Reply(Reply.Tone.ERROR, e.getMessage(), false);
        }
        return new Reply(Reply.Tone.SUCCESS,
                String.format("Started leaking %d MB over %d seconds", megabytes, seconds), true);
    }

    public static Reply stop(LeakManager leak) {
        leak.stop();
        return new Reply(Reply.Tone.WARNING, "Stopped memory leak", true);
    }

    public static Reply clear(LeakManager leak) {
        leak.clear();
        return new Reply(Reply.Tone.SUCCESS, "Cleared all leaked memory", true);
    }

    public static Reply status(LeakManager leak) {
        LeakStatus status = leak.status();
        if (status.leaking()) {
            return new Reply(Reply.Tone.WARNING, String.format(
                    "Leak in progress: %d/%d MB (%.1f%%) - %.1f seconds remaining",
                    status.allocatedMegabytes(), status.targetMegabytes(),
                    status.progress() * 100.0, status.remainingSeconds()), false);
        }
        return new Reply(Reply.Tone.INFO,
                String.format("No active leak. Total leaked: %d MB", status.allocatedMegabytes()), false);
    }

    public static Reply execute(LeakManager leak, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("start") && args.length == 3) {
            try {
                return start(leak, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } catch (NumberFormatException e) {
                return usage();
            }
        }
        if (args.length == 1) {
            switch (sub) {
                case "stop":
                    return stop(leak);
                case "clear":
                    return clear(leak);
                case "status":
                    return status(leak);
                default:
                    break;
            }
        }
        return usage();
    }

    public static List<String> complete(String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(prefix)) {
                matches.add(sub);
            }
        }
        return matches;
    }

    private static Reply usage() {
        return new Reply(Reply.Tone.ERROR, "Usage: " + USAGE, false);
    }
}
