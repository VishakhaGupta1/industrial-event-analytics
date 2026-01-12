package com.industrial.eventanalytics.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

@Entity
@Table(name = "events", 
       uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;
    
    @Column(name = "event_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant eventTime;
    
    @Column(name = "received_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant receivedTime;
    
    @Column(name = "machine_id", nullable = false)
    private String machineId;
    
    @Column(name = "duration_ms", nullable = false)
    @Min(value = 0, message = "Duration must be non-negative")
    @Max(value = 21600000, message = "Duration must not exceed 6 hours")
    private Long durationMs;
    
    @Column(name = "defect_count", nullable = false)
    private Integer defectCount;
    
    // Default constructor for JPA
    public Event() {}
    
    // Constructor for creating new events
    public Event(String eventId, Instant eventTime, Instant receivedTime, 
                 String machineId, Long durationMs, Integer defectCount) {
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.receivedTime = receivedTime;
        this.machineId = machineId;
        this.durationMs = durationMs;
        this.defectCount = defectCount;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public Instant getEventTime() {
        return eventTime;
    }
    
    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }
    
    public Instant getReceivedTime() {
        return receivedTime;
    }
    
    public void setReceivedTime(Instant receivedTime) {
        this.receivedTime = receivedTime;
    }
    
    public String getMachineId() {
        return machineId;
    }
    
    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    
    public Integer getDefectCount() {
        return defectCount;
    }
    
    public void setDefectCount(Integer defectCount) {
        this.defectCount = defectCount;
    }
    
    /**
     * Check if this event should be included in defect calculations
     * @return true if defectCount is not -1 (unknown)
     */
    @JsonIgnore
    public boolean hasValidDefectCount() {
        return defectCount != -1;
    }
    
    /**
     * Check if this event is newer than another event based on receivedTime
     * @param other the event to compare against
     * @return true if this event's receivedTime is newer
     */
    @JsonIgnore
    public boolean isNewerThan(Event other) {
        return this.receivedTime.isAfter(other.getReceivedTime());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Event event = (Event) o;
        return eventId != null ? eventId.equals(event.eventId) : event.eventId == null;
    }
    
    @Override
    public int hashCode() {
        return eventId != null ? eventId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "eventId='" + eventId + '\'' +
                ", eventTime=" + eventTime +
                ", receivedTime=" + receivedTime +
                ", machineId='" + machineId + '\'' +
                ", durationMs=" + durationMs +
                ", defectCount=" + defectCount +
                '}';
    }
}
