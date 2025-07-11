package com.store.controller;

import com.store.dto.UserRegisterRequestDTO;
import com.store.dto.UserLoginRequestDTO;
import com.store.dto.UserLoginResponseDTO;
import com.store.entity.UserEntity;
import com.store.repository.UserRepository;
import com.store.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequestDTO req) {
        if (userRepository.existsByLoginName(req.getLoginName())) {
            return ResponseEntity.badRequest().body("Login name already exists");
        }
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        UserEntity user = new UserEntity();
        user.setLoginName(req.getLoginName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setCreateTime(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequestDTO req) {
        UserEntity user = userRepository.findByLoginName(req.getLoginName())
                .orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid login name or password");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getLoginName());
        return ResponseEntity.ok(new UserLoginResponseDTO(token, user.getId(), user.getLoginName(), user.getName()));
    }
} 