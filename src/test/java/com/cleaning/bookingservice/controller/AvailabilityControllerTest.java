package com.cleaning.bookingservice.controller;

import com.cleaning.bookingservice.service.AvailabilityServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @InjectMocks
    private AvailabilityServiceImpl service;

    @Test
    void testCheckAvailability_ValidationFails() throws Exception {

        String json = """
            {
              "date": "2025-11-16",
              "cleanerCount": 5
            }
            """;

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cleanerCount").value("must be less than or equal to 3"));
    }
}
