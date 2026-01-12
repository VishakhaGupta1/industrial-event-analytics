package com.industrial.eventanalytics.service;

import com.industrial.eventanalytics.dto.BatchResponse;
import com.industrial.eventanalytics.dto.EventRequest;
import com.industrial.eventanalytics.model.Event;
import com.industrial.eventanalytics.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    private static final long MAX_DURATION_MS = 6 * 60 * 60 * 1000L; // 6 hours
    private static final long FUTURE_TOLERANCE_MS = 15 * 60 * 1000L; // 15 minutes
    
    // For thread-safe deduplication tracking during batch processing
    private final ConcurrentHashMap<String, Event> batchCache = new ConcurrentHashMap<>();
    
    @Transactional
    public BatchResponse processBatchEvents(List<EventRequest> eventRequests) {
        int accepted = 0;
        int deduped = 0;
        int updated = 0;
        int rejected = 0;
        List<BatchResponse.RejectionDetail> rejections = new ArrayList<>();
        
        // Clear batch cache for new batch
        batchCache.clear();
        
        for (EventRequest request : eventRequests) {
            try {
                // Validate event
                String validationError = validateEvent(request);
                if (validationError != null) {
                    rejected++;
                    rejections.add(new BatchResponse.RejectionDetail(request.getEventId(), validationError));
                    continue;
                }
                
                // Set received time to provided value or current time if not provided
                Instant requestReceivedTime = request.getReceivedTime() != null ? request.getReceivedTime() : Instant.now();
                Event newEvent = convertToEvent(request, requestReceivedTime);
                
                // Check for existing event
                Optional<Event> existingEventOpt = eventRepository.findByEventId(request.getEventId());
                
                if (existingEventOpt.isPresent()) {
                    Event existingEvent = existingEventOpt.get();
                    
                    // Check if payload is identical (deduplication)
                    if (isPayloadIdentical(newEvent, existingEvent)) {
                        deduped++;
                        continue;
                    }
                    
                    // Different payload - check if newer (update)
                    if (requestReceivedTime.isAfter(existingEvent.getReceivedTime())) {
                        // Update the existing event by changing its properties
                        existingEvent.setEventTime(newEvent.getEventTime());
                        existingEvent.setReceivedTime(requestReceivedTime); // Use request time for the update
                        existingEvent.setMachineId(newEvent.getMachineId());
                        existingEvent.setDurationMs(newEvent.getDurationMs());
                        existingEvent.setDefectCount(newEvent.getDefectCount());
                        eventRepository.save(existingEvent);
                        updated++;
                    } else {
                        // Older payload - ignore
                        deduped++;
                    }
                } else {
                    // New event - save it
                    eventRepository.save(newEvent);
                    accepted++;
                }
                
            } catch (Exception e) {
                rejected++;
                rejections.add(new BatchResponse.RejectionDetail(request.getEventId(), "PROCESSING_ERROR: " + e.getMessage()));
            }
        }
        
        return new BatchResponse(accepted, deduped, updated, rejected, rejections);
    }
    
    private String validateEvent(EventRequest request) {
        // Check for null required fields
        if (request.getEventId() == null || request.getEventId().trim().isEmpty()) {
            return "MISSING_EVENT_ID";
        }
        if (request.getEventTime() == null) {
            return "MISSING_EVENT_TIME";
        }
        if (request.getMachineId() == null || request.getMachineId().trim().isEmpty()) {
            return "MISSING_MACHINE_ID";
        }
        if (request.getDurationMs() == null) {
            return "MISSING_DURATION";
        }
        if (request.getDefectCount() == null) {
            return "MISSING_DEFECT_COUNT";
        }
        
        // Check duration bounds
        if (request.getDurationMs() < 0 || request.getDurationMs() > MAX_DURATION_MS) {
            return "INVALID_DURATION";
        }
        
        // Check if event time is too far in the future
        Instant now = Instant.now();
        if (request.getEventTime().isAfter(now.plusMillis(FUTURE_TOLERANCE_MS))) {
            return "FUTURE_EVENT_TIME";
        }
        
        return null; // Valid
    }
    
    private Event convertToEvent(EventRequest request, Instant receivedTime) {
        return new Event(
            request.getEventId(),
            request.getEventTime(),
            receivedTime,
            request.getMachineId(),
            request.getDurationMs(),
            request.getDefectCount()
        );
    }
    
    private boolean isPayloadIdentical(Event event1, Event event2) {
        return event1.getEventTime().equals(event2.getEventTime()) &&
               event1.getMachineId().equals(event2.getMachineId()) &&
               event1.getDurationMs().equals(event2.getDurationMs()) &&
               event1.getDefectCount().equals(event2.getDefectCount());
        // Note: receivedTime is NOT compared for payload identity as it's set by server
    }
}
