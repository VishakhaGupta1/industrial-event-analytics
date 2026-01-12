package com.industrial.eventanalytics;

import com.industrial.eventanalytics.dto.BatchResponse;
import com.industrial.eventanalytics.dto.EventRequest;
import com.industrial.eventanalytics.dto.StatsResponse;
import com.industrial.eventanalytics.dto.TopDefectLineResponse;
import com.industrial.eventanalytics.model.Event;
import com.industrial.eventanalytics.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventAnalyticsApplicationTests {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MockMvc mockMvc;
    
    @BeforeEach
    @Transactional
    void setUp() {
        eventRepository.deleteAll();
    }
    
    @Test
    void testIdenticalDuplicateEventIdDeduped() throws Exception {
        Instant eventTime = Instant.now().minus(1, ChronoUnit.HOURS);
        EventRequest event = new EventRequest("E-1", eventTime, Instant.now(), 
                                            "M-001", 1000L, 0);
        
        List<EventRequest> events = Arrays.asList(event, event);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.deduped").value(1))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.rejected").value(0));
        
        assertEquals(1, eventRepository.count());
    }
    
    @Test
    void testDifferentPayloadNewerReceivedTimeUpdate() throws Exception {
        Instant eventTime = Instant.now().minus(1, ChronoUnit.HOURS);
        EventRequest event1 = new EventRequest("E-1", eventTime, Instant.now().minus(5, ChronoUnit.MINUTES), 
                                              "M-001", 1000L, 0);
        EventRequest event2 = new EventRequest("E-1", eventTime, Instant.now(), 
                                              "M-001", 1500L, 2);
        
        List<EventRequest> events = Arrays.asList(event1, event2);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.deduped").value(0))
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.rejected").value(0));
        
        assertEquals(1, eventRepository.count());
        Event savedEvent = eventRepository.findByEventId("E-1").orElse(null);
        assertNotNull(savedEvent);
        assertEquals(1500L, savedEvent.getDurationMs());
        assertEquals(2, savedEvent.getDefectCount());
    }
    
    @Test
    void testDifferentPayloadOlderReceivedTimeIgnored() throws Exception {
        Instant eventTime = Instant.now().minus(1, ChronoUnit.HOURS);
        EventRequest event1 = new EventRequest("E-1", eventTime, Instant.now(), 
                                              "M-001", 1000L, 0);
        EventRequest event2 = new EventRequest("E-1", eventTime, Instant.now().minus(5, ChronoUnit.MINUTES), 
                                              "M-001", 1500L, 2);
        
        List<EventRequest> events = Arrays.asList(event1, event2);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.deduped").value(1))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.rejected").value(0));
        
        assertEquals(1, eventRepository.count());
        Event savedEvent = eventRepository.findByEventId("E-1").orElse(null);
        assertNotNull(savedEvent);
        assertEquals(1000L, savedEvent.getDurationMs());
        assertEquals(0, savedEvent.getDefectCount());
    }
    
    @Test
    void testInvalidDurationRejected() throws Exception {
        Instant eventTime = Instant.now().minus(1, ChronoUnit.HOURS);
        EventRequest validEvent = new EventRequest("E-1", eventTime, Instant.now(), 
                                                 "M-001", 1000L, 0);
        EventRequest invalidEvent = new EventRequest("E-2", eventTime, Instant.now(), 
                                                  "M-001", -1L, 0);
        
        List<EventRequest> events = Arrays.asList(validEvent, invalidEvent);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.deduped").value(0))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.rejections[0].eventId").value("E-2"))
                .andExpect(jsonPath("$.rejections[0].reason").value("INVALID_DURATION"));
        
        assertEquals(1, eventRepository.count());
    }
    
    @Test
    void testFutureEventTimeRejected() throws Exception {
        Instant futureEventTime = Instant.now().plus(20, ChronoUnit.MINUTES);
        EventRequest futureEvent = new EventRequest("E-1", futureEventTime, Instant.now(), 
                                               "M-001", 1000L, 0);
        
        List<EventRequest> events = Arrays.asList(futureEvent);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.deduped").value(0))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.rejections[0].eventId").value("E-1"))
                .andExpect(jsonPath("$.rejections[0].reason").value("FUTURE_EVENT_TIME"));
        
        assertEquals(0, eventRepository.count());
    }
    
    @Test
    void testFutureEventTimeRejected() throws Exception {
        Instant futureEventTime = Instant.now().plus(20, ChronoUnit.MINUTES); // 20 minutes in future
        EventRequest futureEvent = new EventRequest("E-1", futureEventTime, Instant.now(), 
                                               "M-001", 1000L, 0);
                                                   "M-001", 1000L, 0);
        
        List<EventRequest> events = Arrays.asList(futureEvent);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.rejections[0].reason").value("FUTURE_EVENT_TIME"));
        
        assertEquals(0, eventRepository.count());
    }
    
    @Test
    void testDefectCountMinusOneIgnoredInDefectTotals() throws Exception {
        Instant eventTime = Instant.now().minus(1, ChronoUnit.HOURS);
        EventRequest eventWithDefects = new EventRequest("E-1", eventTime, Instant.now(), 
                                                       "M-001", 1000L, 5);
        EventRequest eventWithUnknownDefects = new EventRequest("E-2", eventTime, Instant.now(), 
                                                               "M-001", 1000L, -1);
        
        List<EventRequest> events = Arrays.asList(eventWithDefects, eventWithUnknownDefects);
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(2));
        
        Instant start = eventTime.minus(1, ChronoUnit.HOURS);
        Instant end = eventTime.plus(1, ChronoUnit.HOURS);
        
        MvcResult result = mockMvc.perform(get("/api/v1/stats")
                .param("machineId", "M-001")
                .param("start", start.toString())
                .param("end", end.toString()))
                .andExpect(status().isOk())
                .andReturn();
        
        StatsResponse stats = objectMapper.readValue(result.getResponse().getContentAsString(), StatsResponse.class);
        assertEquals(2, stats.getEventsCount()); // Both events counted
        assertEquals(5, stats.getDefectsCount()); // Only the known defects counted
    }
    
    @Test
    void testStartEndBoundaryCorrectness() throws Exception {
        Instant eventTime1 = Instant.parse("2026-01-12T10:00:00Z");
        Instant eventTime2 = Instant.parse("2026-01-12T11:59:59Z");
        Instant eventTime3 = Instant.parse("2026-01-12T12:00:00Z");
        
        EventRequest event1 = new EventRequest("E-1", eventTime1, Instant.now(), "M-001", 1000L, 1);
        EventRequest event2 = new EventRequest("E-2", eventTime2, Instant.now(), "M-001", 1000L, 1);
        EventRequest event3 = new EventRequest("E-3", eventTime3, Instant.now(), "M-001", 1000L, 1);
        
        List<EventRequest> events = Arrays.asList(event1, event2, event3);
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk());
        
        Instant start = Instant.parse("2026-01-12T10:00:00Z");
        Instant end = Instant.parse("2026-01-12T12:00:00Z");
        
        MvcResult result = mockMvc.perform(get("/api/v1/stats")
                .param("machineId", "M-001")
                .param("start", start.toString())
                .param("end", end.toString()))
                .andExpect(status().isOk())
                .andReturn();
        
        StatsResponse stats = objectMapper.readValue(result.getResponse().getContentAsString(), StatsResponse.class);
        assertEquals(3, stats.getEventsCount()); // Should include events at 10:00, 11:59:59, and 12:00
    }
    
    @Test
    void testThreadSafetyConcurrentIngestion() throws Exception {
        int numThreads = 10;
        int eventsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    List<EventRequest> events = new ArrayList<>();
                    for (int j = 0; j < eventsPerThread; j++) {
                        Instant eventTime = Instant.now().minus(1, ChronoUnit.HOURS);
                        EventRequest event = new EventRequest(
                            "E-" + threadId + "-" + j, 
                            eventTime, 
                            Instant.now(), 
                            "M-" + (threadId % 3), // 3 different machines
                            1000L + j, 
                            j % 5
                        );
                        events.add(event);
                    }
                    
                    mockMvc.perform(post("/api/v1/events/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(events)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    fail("Thread " + threadId + " failed: " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long totalEvents = eventRepository.count();
        assertEquals(numThreads * eventsPerThread, totalEvents);
        
        List<String> allEventIds = eventRepository.findAll().stream()
                .map(Event::getEventId)
                .sorted()
                .toList();
        
        List<String> uniqueEventIds = allEventIds.stream().distinct().sorted().toList();
        assertEquals(allEventIds.size(), uniqueEventIds.size());
    }
}
