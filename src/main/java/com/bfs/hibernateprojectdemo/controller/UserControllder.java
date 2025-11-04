package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController

public class UserControllder {

    @Autowired
    private RegisterService registerService;

    @PostMapping("/signup")
    public String registerUser(@RequestParam String userName, @RequestParam String Email, @RequestParam String password) {
        boolean success = registerService.registerUser(userName, Email, password);
        if (success) {
            return "Registration successful";
        } else {
            return "Username or Email already exists";
        }
    }
    @PostMapping("/login")
    public String login(@RequestParam String userName,
                        @RequestParam String password) {
        // login logic here
        return "Login successful!";
    }
}
