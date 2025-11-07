package com.bfs.hibernateprojectdemo.security;

import org.springframework.stereotype.Component;

/**
 * Tracks the single active username allowed to access the system.
 * When a new user logs in, this value is updated; all other tokens are rejected.
 */
@Component
public class ActiveUserRegistry {
    private volatile String activeUsername;

    public synchronized void setActiveUsername(String username) {
        this.activeUsername = username;
    }

    public String getActiveUsername() {
        return activeUsername;
    }
}