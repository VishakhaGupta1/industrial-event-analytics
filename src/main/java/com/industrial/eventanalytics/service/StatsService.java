package com.industrial.eventanalytics.service;

import com.industrial.eventanalytics.dto.StatsResponse;
import com.industrial.eventanalytics.dto.TopDefectLineResponse;
import com.industrial.eventanalytics.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatsService {
    
    @Autowired
    private EventRepository eventRepository;
    
    public StatsResponse getMachineStats(String machineId, Instant start, Instant end) {
        long eventsCount = eventRepository.countEventsByMachineAndTimeWindow(machineId, start, end);
        
        Long defectsCountObj = eventRepository.sumDefectsByMachineAndTimeWindow(machineId, start, end);
        long defectsCount = defectsCountObj != null ? defectsCountObj : 0;
        
        double avgDefectRate = calculateAvgDefectRate(defectsCount, start, end);
        
        String status = avgDefectRate < 2.0 ? "Healthy" : "Warning";
        
        return new StatsResponse(machineId, start, end, eventsCount, defectsCount, avgDefectRate, status);
    }
    
    public List<TopDefectLineResponse> getTopDefectLines(Instant from, Instant to, int limit) {
        List<Object[]> results = eventRepository.findTopDefectLinesByTimeWindow(from, to);
        
        List<TopDefectLineResponse> responses = new ArrayList<>();
        int count = 0;
        
        for (Object[] result : results) {
            if (count >= limit) break;
            
            String lineId = (String) result[0];
            Long totalDefects = (Long) result[1];
            Long eventCount = (Long) result[2];
            
            responses.add(new TopDefectLineResponse(lineId, totalDefects, eventCount, calculateDefectsPercent(totalDefects, eventCount)));
            count++;
        }
        
        return responses;
    }
    
    private double calculateAvgDefectRate(long defectsCount, Instant start, Instant end) {
        // Calculate window duration in hours
        double windowHours = ChronoUnit.SECONDS.between(start, end) / 3600.0;
        
        if (windowHours <= 0) {
            return 0.0;
        }
        
        return defectsCount / windowHours;
    }
}
