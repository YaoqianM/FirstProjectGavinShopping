package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.security.DbUserDetailsService;
import com.bfs.hibernateprojectdemo.security.ActiveUserRegistry;
import com.bfs.hibernateprojectdemo.security.JwtService;
import com.bfs.hibernateprojectdemo.service.LoginService;
import com.bfs.hibernateprojectdemo.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController { // Fixed typo in class name

    private final RegisterService registerService;
    private final LoginService loginService;
    private final JwtService jwtService;
    private final DbUserDetailsService userDetailsService;
    private final ActiveUserRegistry activeUserRegistry;

    @Autowired
    public UserController(RegisterService registerService, LoginService loginService,
                         JwtService jwtService, DbUserDetailsService userDetailsService,
                         ActiveUserRegistry activeUserRegistry) {
        this.registerService = registerService;
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.activeUserRegistry = activeUserRegistry;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (user == null || user.getEmail() == null || user.getUsername() == null || user.getPassword() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        User savedUser = registerService.registerUser(user);
        if (savedUser == null) {
            return ResponseEntity.badRequest().body("Username or Email already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        boolean successful = loginService.authenticate(user.getUsername(), user.getPassword());
        if (!successful) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect credentials, please try again.");
        }
        
        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        // Set active user for single-session enforcement
        activeUserRegistry.setActiveUsername(user.getUsername());
        
        // Return token in response
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("message", "Login successful");
        return ResponseEntity.ok(response);
    }
}