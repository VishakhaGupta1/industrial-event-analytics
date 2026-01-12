package com.industrial.eventanalytics.controller;

import com.industrial.eventanalytics.dto.BatchResponse;
import com.industrial.eventanalytics.dto.EventRequest;
import com.industrial.eventanalytics.dto.StatsResponse;
import com.industrial.eventanalytics.dto.TopDefectLineResponse;
import com.industrial.eventanalytics.service.EventService;
import com.industrial.eventanalytics.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private StatsService statsService;
    
    @PostMapping("/events/batch")
    public ResponseEntity<BatchResponse> ingestBatchEvents(@RequestBody List<EventRequest> events) {
        BatchResponse response = eventService.processBatchEvents(events);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(
            @RequestParam String machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        StatsResponse response = statsService.getMachineStats(machineId, start, end);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats/top-defect-lines")
    public ResponseEntity<List<TopDefectLineResponse>> getTopDefectLines(
            @RequestParam String factoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "10") int limit) {
        
        // Note: factoryId parameter is included for API compatibility but not used in current implementation
        // as machineId serves as the line identifier in this simplified version
        List<TopDefectLineResponse> response = statsService.getTopDefectLines(from, to, limit);
        return ResponseEntity.ok(response);
    }
}
