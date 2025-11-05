package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.User;
import com.bfs.hibernateprojectdemo.security.JwtUtil;
import com.bfs.hibernateprojectdemo.service.LoginService;
import com.bfs.hibernateprojectdemo.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
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
        User u = new User();
        u.setUsername(user.getUsername());  // mapped to DB column userName by @Column(name="userName")
        u.setEmail(user.getEmail());
        u.setPassword(user.getPassword());  // RegisterService will hash it
        User saved = registerService.registerUser(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            return ResponseEntity.badRequest().body("username and password required");
        }
        User db = loginService.findByUsername(user.getUsername());
        if (db == null || !loginService.verifyPassword(db, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect credentials, please try again.");
        }
        String token = JwtUtil.generate(db.getUsername(), db.getRole());
        return ResponseEntity.ok("{\"token\":\"" + token + "\"}");
    }

//    @PostMapping("/login")
//    public ResponseEntity<String> login(@RequestBody User user) {
//        boolean ok = loginService.authenticate(user);
//        if (!ok) return ResponseEntity.status(401).body("Incorrect credentials, please try again.");
//        return ResponseEntity.ok("Login successful");
//    }
}
