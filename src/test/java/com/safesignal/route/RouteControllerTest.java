package com.safesignal.route;

import com.safesignal.route.dto.RouteResponse;
import com.safesignal.route.service.RouteCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteCalculationService routeCalculationService;

    @Test
    void returnsEvaluatedRoutes() throws Exception {
        when(routeCalculationService.calculateRoutesForCoordinates(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()
        )).thenReturn(List.of());

        mockMvc.perform(
                get("/api/routes/evaluate")
                        .param("origin", "19.0760,72.8777")
                        .param("destination", "19.0800,72.8900")
                        .param("hour", "20")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

        verify(routeCalculationService).calculateRoutesForCoordinates(
                19.0760, 72.8777, 19.0800, 72.8900, 20
        );
    }
}