package io.resana;

public class DismissOption {
    private String key;
    private String reason;

    DismissOption(String key, String reason) {
        this.key = key;
        this.reason = reason;
    }

    String getkey() {
        return key;
    }

    public String getReason() {
        return reason;
    }
}
