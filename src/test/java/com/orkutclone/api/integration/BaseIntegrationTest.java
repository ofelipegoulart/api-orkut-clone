package com.orkutclone.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orkutclone.api.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests.
 *
 * Uses the real Spring context with H2 in-memory database.
 * Provides helpers to register users and obtain JWT tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private int userCounter = 0;

    protected AuthResponse registerUser(String name, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>() {{
                    put("name", name);
                    put("email", email);
                    put("password", password);
                }}
        );

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    protected AuthResponse registerUniqueUser() throws Exception {
        userCounter++;
        return registerUser(
                "User " + userCounter,
                "user" + userCounter + "_" + System.nanoTime() + "@orkut.com",
                "senha123"
        );
    }

    protected String authHeader(AuthResponse auth) {
        return "Bearer " + auth.token();
    }
}
