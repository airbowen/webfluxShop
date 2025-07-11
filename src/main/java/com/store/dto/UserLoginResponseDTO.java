package com.store.dto;

public class UserLoginResponseDTO {
    private String token;
    private Long userId;
    private String loginName;
    private String name;

    public UserLoginResponseDTO() {}
    public UserLoginResponseDTO(String token, Long userId, String loginName, String name) {
        this.token = token;
        this.userId = userId;
        this.loginName = loginName;
        this.name = name;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
} 