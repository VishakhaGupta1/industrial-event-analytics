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
    
    private static final long MAX_DURATION_MS = 6 * 60 * 60 * 1000L;
    private static final long FUTURE_TOLERANCE_MS = 15 * 60 * 1000L;
    
    private final ConcurrentHashMap<String, Event> batchCache = new ConcurrentHashMap<>();
    
    @Transactional
    public BatchResponse processBatchEvents(List<EventRequest> eventRequests) {
        int accepted = 0;
        int deduped = 0;
        int updated = 0;
        int rejected = 0;
        List<BatchResponse.RejectionDetail> rejections = new ArrayList<>();
        
        batchCache.clear();
        
        for (EventRequest request : eventRequests) {
            try {
                String validationError = validateEvent(request);
                if (validationError != null) {
                    rejected++;
                    rejections.add(new BatchResponse.RejectionDetail(request.getEventId(), validationError));
                    continue;
                }
                
                Instant requestReceivedTime = request.getReceivedTime() != null ? request.getReceivedTime() : Instant.now();
                Event newEvent = convertToEvent(request, requestReceivedTime);
                
                Optional<Event> existingEventOpt = eventRepository.findByEventId(request.getEventId());
                
                if (existingEventOpt.isPresent()) {
                    Event existingEvent = existingEventOpt.get();
                    
                    if (isPayloadIdentical(newEvent, existingEvent)) {
                        deduped++;
                        continue;
                    }
                    
                    if (requestReceivedTime.isAfter(existingEvent.getReceivedTime())) {
                        existingEvent.setEventTime(newEvent.getEventTime());
                        existingEvent.setReceivedTime(requestReceivedTime);
                        existingEvent.setMachineId(newEvent.getMachineId());
                        existingEvent.setDurationMs(newEvent.getDurationMs());
                        existingEvent.setDefectCount(newEvent.getDefectCount());
                        eventRepository.save(existingEvent);
                        updated++;
                    } else {
                        deduped++;
                    }
                } else {
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
        
        if (request.getDurationMs() < 0 || request.getDurationMs() > MAX_DURATION_MS) {
            return "INVALID_DURATION";
        }
        
        Instant now = Instant.now();
        if (request.getEventTime().isAfter(now.plusMillis(FUTURE_TOLERANCE_MS))) {
            return "FUTURE_EVENT_TIME";
        }
        
        return null;
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
    }
}
