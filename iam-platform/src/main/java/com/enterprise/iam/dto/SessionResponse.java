package com.enterprise.iam.dto;

public class SessionResponse {
    private Long id;
    private String username;
    private String expiryDate;

    public SessionResponse() {
    }

    public SessionResponse(Long id, String username, String expiryDate) {
        this.id = id;
        this.username = username;
        this.expiryDate = expiryDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}
