package com.bfs.hibernateprojectdemo.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private Long resourceId;

    @Column
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String changeSet;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Long previousVersion;

    @Column
    private Long newVersion;

    // getters/setters
    public Long getId() { return id; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getChangeSet() { return changeSet; }
    public void setChangeSet(String changeSet) { this.changeSet = changeSet; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Long getPreviousVersion() { return previousVersion; }
    public void setPreviousVersion(Long previousVersion) { this.previousVersion = previousVersion; }
    public Long getNewVersion() { return newVersion; }
    public void setNewVersion(Long newVersion) { this.newVersion = newVersion; }
}