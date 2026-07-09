package com.enterprise.iam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private boolean mfaRequired;
    private String mfaToken;
    private String username;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, boolean mfaRequired, String mfaToken, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mfaRequired = mfaRequired;
        this.mfaToken = mfaToken;
        this.username = username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static class LoginResponseBuilder {
        private String accessToken;
        private String refreshToken;
        private boolean mfaRequired;
        private String mfaToken;
        private String username;

        public LoginResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public LoginResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public LoginResponseBuilder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public LoginResponseBuilder mfaToken(String mfaToken) {
            this.mfaToken = mfaToken;
            return this;
        }

        public LoginResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginResponse build() {
            return new LoginResponse(accessToken, refreshToken, mfaRequired, mfaToken, username);
        }
    }

    public static LoginResponseBuilder builder() {
        return new LoginResponseBuilder();
    }
}
