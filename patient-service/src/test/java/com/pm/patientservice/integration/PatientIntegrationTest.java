package com.pm.patientservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.kafkaProducer;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Full integration test: real HTTP → real Service → real Repository → real PostgreSQL.
//
// BillingServiceGrpcClient and kafkaProducer are @MockitoBean (replaces deprecated @MockBean)
// because they depend on out-of-process infrastructure (billing-service, Kafka broker)
// that is not part of this test's scope.
//
// Exception → HTTP mapping tests are excluded — they depend on a @ControllerAdvice
// that does not exist yet in your codebase. The tests that ARE here exercise
// the full stack for all happy paths and the DB-level duplicate email constraint.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PatientIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("patient_integration_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Prevent Kafka auto-configuration from failing at context startup.
        // kafkaProducer is @MockitoBean so Kafka infra is never actually called.
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PatientRepository patientRepository;

    @MockitoBean private BillingServiceGrpcClient billingServiceGrpcClient;
    @MockitoBean private kafkaProducer kafkaProducer;

    @BeforeEach
    void cleanDatabase() {
        patientRepository.deleteAll();
    }

    private PatientRequestDTO buildCreateRequest(String name, String email) {
        PatientRequestDTO dto = new PatientRequestDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setAddress("123 Integration Test St");
        dto.setDateOfBirth("1990-06-15");
        dto.setRegisteredDate("2024-01-10");
        return dto;
    }

    @Test
    @DisplayName("POST /patients — patient is persisted and returned with generated ID")
    void createPatient_persistsAndReturnsId() throws Exception {
        PatientRequestDTO request = buildCreateRequest("John Doe", "john@example.com");

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.address").value("123 Integration Test St"));

        assertThat(patientRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST then GET by ID — created patient is retrievable")
    void createThenGetById_patientIsRetrievable() throws Exception {
        PatientRequestDTO request = buildCreateRequest("John Doe", "john@example.com");

        MvcResult createResult = mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST then PUT then GET — updated values are persisted to DB")
    void createThenUpdate_updatedValuesPersistedToDb() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreateRequest("Original Name", "original@example.com"))))
                .andExpect(status().isOk())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // registeredDate not required on PUT
        PatientRequestDTO updateRequest = new PatientRequestDTO();
        updateRequest.setName("Updated Name");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setAddress("999 Updated Street");
        updateRequest.setDateOfBirth("1990-06-15");

        mockMvc.perform(put("/patients/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        // Confirm DB reflects the update, not just the HTTP response
        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.address").value("999 Updated Street"));
    }

    @Test
    @DisplayName("POST then DELETE — patient is removed from DB")
    void createThenDelete_patientRemovedFromDb() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreateRequest("To Delete", "delete.me@example.com"))))
                .andExpect(status().isOk())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/patients/{id}", id))
                .andExpect(status().isNoContent());

        // Verify DB directly — not just the HTTP response
        assertThat(patientRepository.findById(UUID.fromString(id))).isEmpty();
    }

    @Test
    @DisplayName("POST duplicate email — service throws, DB stays at 1 record")
    void createDuplicateEmail_dbRemainsConsistent() throws Exception {
        PatientRequestDTO first = buildCreateRequest("First Patient", "shared@example.com");
        PatientRequestDTO second = buildCreateRequest("Second Patient", "shared@example.com");

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        // Second request throws EmailAlreadyExistsException.
        // Without a @ControllerAdvice this returns 500.
        // The important thing to verify: the DB has exactly 1 record.
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)));

        assertThat(patientRepository.findAll()).hasSize(1);
        assertThat(patientRepository.existsByEmail("shared@example.com")).isTrue();
    }

    @Test
    @DisplayName("GET /patients — returns all created patients")
    void getPatients_returnsAllCreatedPatients() throws Exception {
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Alice", "alice@example.com"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Bob", "bob@example.com"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email",
                        containsInAnyOrder("alice@example.com", "bob@example.com")));
    }
}