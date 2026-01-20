package org.luigilp.lPChestShop.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class GitHubReleaseUpdateChecker {

    private GitHubReleaseUpdateChecker() {
    }

    public static void check(JavaPlugin plugin, String owner, String repo) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String current = normalizeVersion(plugin.getDescription().getVersion());
            String latestTag = fetchLatestReleaseTag(owner, repo);
            if (latestTag == null || latestTag.isBlank()) return;

            String latest = normalizeVersion(latestTag);
            int cmp = compareVersionNumbers(latest, current);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (cmp > 0) {
                    plugin.getLogger().severe("You are not on the latest version! Current: " + current + " Newest: " + latest);
                } else {
                    plugin.getLogger().info("You have the latest version. Current: " + current);
                }
            });
        });
    }

    private static String fetchLatestReleaseTag(String owner, String repo) {
        try {
            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", repo + "-UpdateChecker")
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return null;

            String body = resp.body();
            if (body == null || body.isBlank()) return null;

            return extractJsonStringField(body, "tag_name");
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String extractJsonStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int i = json.indexOf(key);
        if (i < 0) return null;
        i = json.indexOf(':', i + key.length());
        if (i < 0) return null;
        int start = json.indexOf('"', i + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end).trim();
    }

    private static String normalizeVersion(String v) {
        if (v == null) return "0.0.0";
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int plus = s.indexOf('+');
        if (plus >= 0) s = s.substring(0, plus);
        return s.trim();
    }

    private static int compareVersionNumbers(String a, String b) {
        List<Integer> pa = parseNumbers(a);
        List<Integer> pb = parseNumbers(b);
        int max = Math.max(pa.size(), pb.size());
        for (int i = 0; i < max; i++) {
            int ai = i < pa.size() ? pa.get(i) : 0;
            int bi = i < pb.size() ? pb.get(i) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static List<Integer> parseNumbers(String s) {
        List<Integer> out = new ArrayList<>();
        if (s == null) {
            out.add(0);
            return out;
        }

        int n = s.length();
        int cur = 0;
        boolean inNum = false;

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                inNum = true;
                cur = cur * 10 + (c - '0');
            } else {
                if (inNum) {
                    out.add(cur);
                    cur = 0;
                    inNum = false;
                }
            }
        }

        if (inNum) out.add(cur);
        if (out.isEmpty()) out.add(0);
        return out;
    }
}
