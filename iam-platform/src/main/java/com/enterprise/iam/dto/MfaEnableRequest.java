package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaEnableRequest {
    @NotBlank(message = "Secret is required")
    private String secret;

    @NotBlank(message = "TOTP code is required")
    private String code;

    public MfaEnableRequest() {
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
