package com.safesignal.route;

import com.safesignal.route.dto.LocationSummaryResponse;
import com.safesignal.route.dto.RouteResponse;
import com.safesignal.route.dto.RouteSegmentResponse;
import com.safesignal.route.dto.StreetSegmentResponse;
import com.safesignal.route.service.RouteCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
@ActiveProfiles("mysql")
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteCatalogService routeCatalogService;

    @Test
    void returnsFilteredRoutesWithOrderedSegments() throws Exception {

        RouteResponse route = new RouteResponse(
                1L,
                "Market Direct",
                18,
                1280,
                new LocationSummaryResponse(
                        1L,
                        "Central Mall",
                        12.9716000,
                        77.5946000
                ),
                new LocationSummaryResponse(
                        2L,
                        "Riverside Apartments",
                        12.9635000,
                        77.6012000
                ),
                List.of(
                        new RouteSegmentResponse(
                                1,
                                new StreetSegmentResponse(
                                        1L,
                                        "Central Avenue",
                                        12.9716000,
                                        77.5946000,
                                        12.9701000,
                                        77.5961000
                                )
                        ),
                        new RouteSegmentResponse(
                                2,
                                new StreetSegmentResponse(
                                        2L,
                                        "Market Lane",
                                        12.9701000,
                                        77.5961000,
                                        12.9678000,
                                        77.5983000
                                )
                        )
                ),
                List.of()
        );

        when(routeCatalogService.getRoutes(1L, 2L))
                .thenReturn(List.of(route));

        mockMvc.perform(
                        get("/api/routes")
                                .param("originId", "1")
                                .param("destinationId", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Market Direct"))
                .andExpect(jsonPath("$[0].segments[0].sequenceOrder").value(1))
                .andExpect(jsonPath("$[0].segments[1].streetSegment.name")
                        .value("Market Lane"));

        verify(routeCatalogService).getRoutes(1L, 2L);
    }

    @Test
    void returnsEmptyListWhenNoRoutesMatch() throws Exception {

        when(routeCatalogService.getRoutes(99L, 100L))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/routes")
                                .param("originId", "99")
                                .param("destinationId", "100")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}