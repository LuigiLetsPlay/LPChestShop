package org.luigilp.lPChestShop.util;

public final class MoneyParser {

    private MoneyParser() {}

    public static long parseToLong(String input) {
        String s = input.trim().toLowerCase();
        if (s.isEmpty()) throw new IllegalArgumentException("empty");

        long mult = 1;
        if (s.endsWith("k")) {
            mult = 1_000L;
            s = s.substring(0, s.length() - 1).trim();
        } else if (s.endsWith("m")) {
            mult = 1_000_000L;
            s = s.substring(0, s.length() - 1).trim();
        }

        double val = Double.parseDouble(s);
        long out = (long) Math.floor(val * mult);
        if (out < 0) throw new IllegalArgumentException("negative");
        return out;
    }
}
