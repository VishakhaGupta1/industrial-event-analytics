package com.industrial.eventanalytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventRequest {
    
    private String eventId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant eventTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant receivedTime;
    
    private String machineId;
    
    private Long durationMs;
    
    private Integer defectCount;
    
    public EventRequest() {}
    
    public EventRequest(String eventId, Instant eventTime, Instant receivedTime, 
                       String machineId, Long durationMs, Integer defectCount) {
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.receivedTime = receivedTime;
        this.machineId = machineId;
        this.durationMs = durationMs;
        this.defectCount = defectCount;
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
}
