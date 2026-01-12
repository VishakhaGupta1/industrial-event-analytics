package com.industrial.eventanalytics.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TopDefectLineResponse {
    private String lineId;
    private long totalDefects;
    private long eventCount;
    private double defectsPercent;
    
    public TopDefectLineResponse() {}
    
    public TopDefectLineResponse(String lineId, long totalDefects, long eventCount) {
        this.lineId = lineId;
        this.totalDefects = totalDefects;
        this.eventCount = eventCount;
        this.defectsPercent = calculateDefectsPercent(totalDefects, eventCount);
    }
    
    private double calculateDefectsPercent(long totalDefects, long eventCount) {
        if (eventCount == 0) {
            return 0.0;
        }
        BigDecimal defects = BigDecimal.valueOf(totalDefects);
        BigDecimal events = BigDecimal.valueOf(eventCount);
        BigDecimal percent = defects
                .divide(events, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return percent.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    
    public String getLineId() {
        return lineId;
    }
    
    public void setLineId(String lineId) {
        this.lineId = lineId;
    }
    
    public long getTotalDefects() {
        return totalDefects;
    }
    
    public void setTotalDefects(long totalDefects) {
        this.totalDefects = totalDefects;
    }
    
    public long getEventCount() {
        return eventCount;
    }
    
    public void setEventCount(long eventCount) {
        this.eventCount = eventCount;
    }
    
    public double getDefectsPercent() {
        return defectsPercent;
    }
    
    public void setDefectsPercent(double defectsPercent) {
        this.defectsPercent = defectsPercent;
    }
}
