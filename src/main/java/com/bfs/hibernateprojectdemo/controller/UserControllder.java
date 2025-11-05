package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.service.LoginService;
import com.bfs.hibernateprojectdemo.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class UserControllder {


    private final RegisterService registerService;
    private final LoginService loginService;

    @Autowired
    public UserControllder(RegisterService registerService, LoginService loginService) {
        this.registerService = registerService;
        this.loginService = loginService;
    }
    @PostMapping("/signup")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.badRequest().build();
        }
        User savedUser = registerService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        // login logic here
        boolean successful = loginService.authenticate(user);
        if (!successful) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        return ResponseEntity.ok("Login successful");
    }
}
