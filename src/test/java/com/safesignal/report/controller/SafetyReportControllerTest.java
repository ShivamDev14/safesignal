package com.safesignal.report.controller;

import com.safesignal.report.dto.SafetyReportResponse;
import com.safesignal.report.model.IncidentType;
import com.safesignal.report.model.ReportStatus;
import com.safesignal.report.service.SafetyReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SafetyReportController.class)
class SafetyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SafetyReportService safetyReportService;

    @Test
    void createsSafetyReport() throws Exception {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 11, 23, 0);
        when(safetyReportService.createReport(any())).thenReturn(new SafetyReportResponse(
                10L, 2L, IncidentType.HARASSMENT, 4, occurredAt, occurredAt,
                "Unwanted attention near the market.", ReportStatus.PENDING));

        String request = """
                {
                  "streetSegmentId": 2,
                  "incidentType": "HARASSMENT",
                  "severity": 4,
                  "occurredAt": "2026-09-11T23:00:00",
                  "description": "Unwanted attention near the market."
                }
                """;

        mockMvc.perform(post("/api/safety-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.streetSegmentId").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsRequestWithoutStreetSegmentId() throws Exception {
        String request = """
                {
                  "incidentType": "HARASSMENT",
                  "severity": 4,
                  "occurredAt": "2026-09-11T23:00:00",
                  "description": "Unwanted attention near the market."
                }
                """;

        mockMvc.perform(post("/api/safety-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.streetSegmentId").value("streetSegmentId is required"));
    }
}
