package com.hezaerd.memoryleak.core;

public final class Reply {
    public enum Tone { SUCCESS, WARNING, INFO, ERROR }

    private final Tone tone;
    private final String text;
    private final boolean notifyAdmins;

    Reply(Tone tone, String text, boolean notifyAdmins) {
        this.tone = tone;
        this.text = text;
        this.notifyAdmins = notifyAdmins;
    }

    public Tone tone() {
        return tone;
    }

    public String text() {
        return text;
    }

    public boolean notifyAdmins() {
        return notifyAdmins;
    }

    public boolean succeeded() {
        return tone != Tone.ERROR;
    }
}
