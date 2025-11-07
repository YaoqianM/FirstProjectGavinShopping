package com.bfs.hibernateprojectdemo.security;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ActiveUserRegistry {
    private final AtomicReference<String> activeUsername = new AtomicReference<>();

    public String getActiveUsername() {
        return activeUsername.get();
    }

    public void setActiveUsername(String username) {
        activeUsername.set(username);
    }

    public void clearActiveUsername() {
        activeUsername.set(null);
    }
}