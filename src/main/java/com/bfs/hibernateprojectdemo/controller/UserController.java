package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.security.DbUserDetailsService;
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

    @Autowired
    public UserController(RegisterService registerService, LoginService loginService,
                         JwtService jwtService, DbUserDetailsService userDetailsService) {
        this.registerService = registerService;
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (user == null || user.getEmail() == null || user.getUserName() == null || user.getPassword() == null) {
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
        boolean successful = loginService.authenticate(user.getUserName(), user.getPassword());
        if (!successful) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect credentials");
        }
        
        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUserName());
        String token = jwtService.generateToken(userDetails);
        
        // Return token in response
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }
}