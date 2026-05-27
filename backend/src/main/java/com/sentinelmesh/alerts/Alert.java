package com.sentinelmesh.alerts;

import java.time.Instant;
import java.util.UUID;

import com.sentinelmesh.events.SecurityEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name="alerts")
public class Alert 
{
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;
	
	@OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_event_id", nullable = false, unique = true)
    private SecurityEvent securityEvent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus status;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Alert() 
    {
        //Required by JPA
    }
    
    public Alert(
    		SecurityEvent securityEvent, 
    		AlertSeverity severity, 
    		String title, 
    		String message) 
    {
        this.securityEvent = securityEvent;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.status = AlertStatus.OPEN;
        this.createdAt = Instant.now();
    }
    
    @PrePersist
    public void prePersist()
    {
    	if(status==null)
    		status=AlertStatus.OPEN;
    	
    	if(createdAt==null)
    		createdAt=Instant.now();
    }
    
    public UUID getId()
    {
    	return id;
    }
    
    public SecurityEvent getSecurityEvent()
    {
    	return securityEvent;
    }
    
    public AlertSeverity getSeverity()
    {
    	return severity;
    }
    
    public AlertStatus getStatus()
    {
    	return status;
    }
    
    public String getTitle()
    {
    	return title;
    }
    
    public String getMessage()
    {
    	return message;
    }
    
    public Instant getCreatedAt()
    {
    	return createdAt;
    }
    
    public Instant getAcknowledgedAt()
    {
    	return acknowledgedAt;
    }
    
    public Instant getResolvedAt()
    {
    	return resolvedAt;
    }
    
    public void acknowledge()
    {
    	this.status=AlertStatus.ACKNOWLEDGED;
    	this.acknowledgedAt=Instant.now();
    }
    
    public void resolve()
    {
    	this.status=AlertStatus.RESOLVED;
    	this.resolvedAt=Instant.now();
    }
}
