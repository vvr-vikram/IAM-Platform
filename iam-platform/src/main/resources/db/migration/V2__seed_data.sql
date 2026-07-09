-- V2__seed_data.sql
-- Seed Permissions
INSERT INTO permissions (name, description) VALUES
('user:read', 'Read user profiles and details'),
('user:write', 'Create and edit user profiles'),
('admin:read', 'Access administrative dashboards and user directories'),
('admin:write', 'Modify system settings, roles, permissions and lock states'),
('audit:read', 'View system audit logs');

-- Seed Roles
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'System Administrator with full access'),
('ROLE_MANAGER', 'Security Manager with user management and audit access'),
('ROLE_USER', 'Standard Employee user');

-- Map Roles to Permissions
-- ROLE_ADMIN gets all permissions (ids 1, 2, 3, 4, 5)
INSERT INTO roles_permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5);

-- ROLE_MANAGER gets user:read, user:write, audit:read (ids 1, 2, 5)
INSERT INTO roles_permissions (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 5);

-- ROLE_USER gets user:read (id 1)
INSERT INTO roles_permissions (role_id, permission_id) VALUES
(3, 1);

-- Seed Default Admin User (username: admin, email: admin@enterprise.com, password: Admin@123)
-- BCrypt hash of 'Admin@123' is '$2a$10$LmoXEwEEGVsS8Q9cEkJiTeD6dFl/AJD6zAjiDvuPNukRO6vsnOMA.'
INSERT INTO users (username, email, password, enabled, email_verified, mfa_enabled, mfa_secret, failed_login_attempts, locked) VALUES
('admin', 'admin@enterprise.com', '$2a$10$LmoXEwEEGVsS8Q9cEkJiTeD6dFl/AJD6zAjiDvuPNukRO6vsnOMA.', TRUE, TRUE, FALSE, NULL, 0, FALSE);

-- Assign ROLE_ADMIN (id 1) to admin (id 1)
INSERT INTO users_roles (user_id, role_id) VALUES
(1, 1);
