package com.enterprise.iam.dto;

import java.util.List;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private boolean mfaEnabled;
    private boolean enabled;
    private boolean emailVerified;
    private boolean locked;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String username, String email, List<String> roles, boolean mfaEnabled, boolean enabled, boolean emailVerified, boolean locked) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.mfaEnabled = mfaEnabled;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.locked = locked;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public static class UserProfileResponseBuilder {
        private Long id;
        private String username;
        private String email;
        private List<String> roles;
        private boolean mfaEnabled;
        private boolean enabled;
        private boolean emailVerified;
        private boolean locked;

        public UserProfileResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserProfileResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserProfileResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileResponseBuilder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public UserProfileResponseBuilder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public UserProfileResponseBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserProfileResponseBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserProfileResponseBuilder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public UserProfileResponse build() {
            return new UserProfileResponse(id, username, email, roles, mfaEnabled, enabled, emailVerified, locked);
        }
    }

    public static UserProfileResponseBuilder builder() {
        return new UserProfileResponseBuilder();
    }
}
