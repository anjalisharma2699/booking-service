package com.cleaning.bookingservice.controller;

import com.cleaning.bookingservice.dto.response.BookingResponse;
import com.cleaning.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;     // ✅ CORRECT
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath; // ✅ CORRECT
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BookingControllerTest.TestConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ObjectMapper mapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public BookingService bookingService() {
            return mock(BookingService.class);
        }
    }

    @Test
    void testCreateBooking() throws Exception {

        String json = """
        {
          "date": "2025-11-16",
          "startTime": "18:00",
          "durationHours": 4,
          "requestedCleanerCount": 3
        }
        """;

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateBooking_ControllerSuccess() throws Exception {

        BookingResponse mockResponse = new BookingResponse();
        mockResponse.setBookingId(10L);
        mockResponse.setAssignedVehicleId(1L);
        mockResponse.setStartDatetime(LocalDateTime.parse("2025-11-16T18:00"));
        mockResponse.setAssignedCleanerIds(List.of(1L, 2L));

        when(bookingService.createBooking(any())).thenReturn(mockResponse);

        String json = """
        {
          "date": "2025-11-16",
          "startTime": "18:00",
          "durationHours": 4,
          "requestedCleanerCount": 2
        }
        """;

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(10L))
                .andExpect(jsonPath("$.assignedCleanerIds.length()").value(2));
    }
}
