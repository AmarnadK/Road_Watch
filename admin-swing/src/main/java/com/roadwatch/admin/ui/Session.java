package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.ApiClient;
import com.roadwatch.admin.api.Models;

/** Who is logged in and the client they use to talk to the server. */
public final class Session {

    private static Session current;

    private final ApiClient api;
    private final Models.AuthResponse user;
    private Runnable onExpire = () -> { };

    private Session(ApiClient api, Models.AuthResponse user) {
        this.api = api;
        this.user = user;
    }

    public static Session current() { return current; }
    public static void start(ApiClient api, Models.AuthResponse user) { current = new Session(api, user); }
    public static void end() { current = null; }

    public ApiClient api() { return api; }
    public Models.AuthResponse user() { return user; }
    public boolean isSuperAdmin() { return "SUPER_ADMIN".equals(user.role()); }

    public void setOnExpire(Runnable r) { this.onExpire = r; }
    public void expire() { onExpire.run(); }
}
