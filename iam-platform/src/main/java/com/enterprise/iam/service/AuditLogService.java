package com.enterprise.iam.service;

import com.enterprise.iam.entity.AuditLog;
import com.enterprise.iam.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long userId, String username, String action, String status, String details) {
        String ipAddress = "unknown";
        String userAgent = "unknown";
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
        }

        AuditLog logEntry = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .status(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .build();
                
        auditLogRepository.save(logEntry);
    }
}
