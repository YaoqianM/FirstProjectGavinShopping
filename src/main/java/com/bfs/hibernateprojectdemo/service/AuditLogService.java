package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.domain.AuditLog;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {
    private final SessionFactory sessionFactory;

    public AuditLogService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void log(String resourceType, Long resourceId, Long userId, String changeSet,
                    Long previousVersion, Long newVersion) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            AuditLog log = new AuditLog();
            log.setResourceType(resourceType);
            log.setResourceId(resourceId);
            log.setUserId(userId);
            log.setChangeSet(changeSet);
            log.setTimestamp(LocalDateTime.now());
            log.setPreviousVersion(previousVersion);
            log.setNewVersion(newVersion);
            s.save(log);
            tx.commit();
        }
    }
}