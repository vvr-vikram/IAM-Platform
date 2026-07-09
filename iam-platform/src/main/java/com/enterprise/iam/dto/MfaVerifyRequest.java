package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaVerifyRequest {
    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "TOTP code is required")
    private String code;

    public MfaVerifyRequest() {
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
