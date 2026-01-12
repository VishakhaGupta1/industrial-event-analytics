package com.industrial.eventanalytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testBatchEndpointAcceptsValidRequest() throws Exception {
        String validBatch = """
            [
                {
                    "eventId": "E-1",
                    "eventTime": "2026-01-15T10:00:00.000Z",
                    "receivedTime": "2026-01-15T10:00:01.000Z",
                    "machineId": "M-001",
                    "durationMs": 1000,
                    "defectCount": 0
                },
                {
                    "eventId": "E-2",
                    "eventTime": "2026-01-15T10:01:00.000Z",
                    "receivedTime": "2026-01-15T10:00:01.000Z",
                    "machineId": "M-001",
                    "durationMs": 1500,
                    "defectCount": -1
                }
            ]
            """;
        
        mockMvc.perform(post("/api/v1/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBatch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").exists())
                .andExpect(jsonPath("$.deduped").exists())
                .andExpect(jsonPath("$.updated").exists())
                .andExpect(jsonPath("$.rejected").exists());
    }
    
    @Test
    void testStatsEndpointReturnsCorrectStructure() throws Exception {
        mockMvc.perform(get("/api/v1/stats")
                .param("machineId", "M-001")
                .param("start", "2026-01-15T00:00:00Z")
                .param("end", "2026-01-15T06:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineId").value("M-001"))
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists())
                .andExpect(jsonPath("$.eventsCount").exists())
                .andExpect(jsonPath("$.defectsCount").exists())
                .andExpect(jsonPath("$.avgDefectRate").exists())
                .andExpect(jsonPath("$.status").exists());
    }
    
    @Test
    void testTopDefectLinesEndpointReturnsCorrectStructure() throws Exception {
        mockMvc.perform(get("/api/v1/stats/top-defect-lines")
                .param("factoryId", "F01")
                .param("from", "2026-01-15T00:00:00Z")
                .param("to", "2026-01-15T23:59:59Z")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
