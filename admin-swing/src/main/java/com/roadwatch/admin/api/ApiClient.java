package com.roadwatch.admin.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** All communication with the Road Watch server. Methods block, so call them off the UI thread (see Async). */
public class ApiClient {

    private static final Gson GSON = new Gson();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private String baseUrl;
    private String token;

    public ApiClient(String baseUrl) {
        String u = baseUrl.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        this.baseUrl = u;
    }

    public String getBaseUrl() { return baseUrl; }

    // ---------------------------------------------------------------- auth

    public Models.AuthResponse login(String username, String password) {
        token = null;
        Models.AuthResponse r = parse(exchange("POST", "/api/auth/login",
                Map.of("username", username, "password", password)), Models.AuthResponse.class);
        token = r.token();
        return r;
    }

    /** Returns the server's confirmation message. */
    public String register(String username, String password, String fullName, long panchayatId) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("fullName", fullName);
        body.put("panchayatId", panchayatId);
        JsonObject o = JsonParser.parseString(text(exchange("POST", "/api/auth/register", body))).getAsJsonObject();
        return o.get("message").getAsString();
    }

    // ---------------------------------------------------------------- reports

    public List<Models.Panchayat> panchayats() {
        return parse(exchange("GET", "/api/panchayats", null), new TypeToken<List<Models.Panchayat>>() { }.getType());
    }

    public List<Models.Report> reports(Long panchayatId, String status) {
        StringBuilder q = new StringBuilder("/api/reports");
        String sep = "?";
        if (panchayatId != null) { q.append(sep).append("panchayatId=").append(panchayatId); sep = "&"; }
        if (status != null) { q.append(sep).append("status=").append(URLEncoder.encode(status, StandardCharsets.UTF_8)); }
        return parse(exchange("GET", q.toString(), null), new TypeToken<List<Models.Report>>() { }.getType());
    }

    public Models.Report report(long id) {
        return parse(exchange("GET", "/api/reports/" + id, null), Models.Report.class);
    }

    public byte[] photo(long id) {
        return exchange("GET", "/api/reports/" + id + "/photo", null).body();
    }

    public Models.Report changeStatus(long id, String status, String note) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if (note != null && !note.isBlank()) body.put("note", note.trim());
        return parse(exchange("PATCH", "/api/reports/" + id + "/status", body), Models.Report.class);
    }

    // ---------------------------------------------------------------- district admin

    public List<Models.User> users(boolean pendingOnly) {
        return parse(exchange("GET", "/api/admin/users?pending=" + pendingOnly, null),
                new TypeToken<List<Models.User>>() { }.getType());
    }

    public Models.User approve(long userId) {
        return parse(exchange("POST", "/api/admin/users/" + userId + "/approve", null), Models.User.class);
    }

    public Models.User deactivate(long userId) {
        return parse(exchange("POST", "/api/admin/users/" + userId + "/deactivate", null), Models.User.class);
    }

    public Models.Panchayat createPanchayat(String name, String type) {
        return parse(exchange("POST", "/api/admin/panchayats", Map.of("name", name, "type", type)),
                Models.Panchayat.class);
    }

    // ---------------------------------------------------------------- plumbing

    private HttpResponse<byte[]> exchange(String method, String path, Object body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30));
            if (token != null) b.header("Authorization", "Bearer " + token);
            if (body != null) {
                b.header("Content-Type", "application/json");
                b.method(method, BodyPublishers.ofString(GSON.toJson(body)));
            } else {
                b.method(method, BodyPublishers.noBody());
            }
            HttpResponse<byte[]> res = http.send(b.build(), BodyHandlers.ofByteArray());
            if (res.statusCode() >= 400) {
                throw new ApiException(res.statusCode(), errorMessage(res));
            }
            return res;
        } catch (IOException e) {
            throw new ApiException(0, "Cannot reach the server. Check the address and your connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "The request was interrupted.");
        } catch (IllegalArgumentException e) {
            throw new ApiException(0, "The server address is not valid.");
        }
    }

    private static String text(HttpResponse<byte[]> res) {
        return new String(res.body(), StandardCharsets.UTF_8);
    }

    private static <T> T parse(HttpResponse<byte[]> res, java.lang.reflect.Type type) {
        return GSON.fromJson(text(res), type);
    }

    private static String errorMessage(HttpResponse<byte[]> res) {
        try {
            JsonElement e = JsonParser.parseString(text(res));
            if (e.isJsonObject() && e.getAsJsonObject().has("error")) {
                return e.getAsJsonObject().get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
            // fall through to generic messages
        }
        return switch (res.statusCode()) {
            case 401 -> "Please log in again.";
            case 403 -> "You are not allowed to do that.";
            case 404 -> "Not found.";
            default -> "Server error (" + res.statusCode() + ").";
        };
    }
}
