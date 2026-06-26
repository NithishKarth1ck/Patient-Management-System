package com.pm.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @MockitoBean replaces the deprecated @MockBean (removed in Spring Boot 3.4+)
// import org.springframework.test.context.bean.override.mockito.MockitoBean  <-- new package
//
// These tests cover ONLY what your actual code guarantees:
//   - Happy path HTTP responses (2xx)
//   - Bean validation failures (400) — Spring handles these, no exception handler needed
//   - Service method invocation verification
//
// Exception → HTTP status mapping (404, 409) is intentionally excluded here.
// That mapping requires a @ControllerAdvice. Add one when you're ready and
// write those tests then.
@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    private PatientResponseDTO responseDTO;
    private PatientRequestDTO validCreateRequest;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();

        responseDTO = new PatientResponseDTO();
        responseDTO.setId(patientId.toString());
        responseDTO.setName("John Doe");
        responseDTO.setEmail("john@example.com");
        responseDTO.setAddress("123 Main St");
        responseDTO.setDateOfBirth("1990-06-15");

        validCreateRequest = new PatientRequestDTO();
        validCreateRequest.setName("John Doe");
        validCreateRequest.setEmail("john@example.com");
        validCreateRequest.setAddress("123 Main St");
        validCreateRequest.setDateOfBirth("1990-06-15");
        validCreateRequest.setRegisteredDate("2024-01-10");
    }

    @Nested
    @DisplayName("GET /patients")
    class GetPatientsTests {

        @Test
        @DisplayName("returns 200 with list of patients")
        void getPatients_returnsOkWithList() throws Exception {
            when(patientService.getPatients()).thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/patients"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].email").value("john@example.com"))
                    .andExpect(jsonPath("$[0].name").value("John Doe"));
        }

        @Test
        @DisplayName("returns 200 with empty array when no patients exist")
        void getPatients_emptyList_returnsOkWithEmptyArray() throws Exception {
            when(patientService.getPatients()).thenReturn(List.of());

            mockMvc.perform(get("/patients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("calls patientService.getPatients() exactly once")
        void getPatients_delegatesToService() throws Exception {
            when(patientService.getPatients()).thenReturn(List.of());

            mockMvc.perform(get("/patients"));

            verify(patientService, times(1)).getPatients();
        }
    }

    @Nested
    @DisplayName("GET /patients/{id}")
    class GetPatientByIdTests {

        @Test
        @DisplayName("returns 200 with patient DTO when found")
        void getPatientById_found_returnsOk() throws Exception {
            when(patientService.getPatientById(patientId)).thenReturn(responseDTO);

            mockMvc.perform(get("/patients/{id}", patientId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(patientId.toString()))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.address").value("123 Main St"));
        }

        @Test
        @DisplayName("passes the correct UUID to the service")
        void getPatientById_passesCorrectIdToService() throws Exception {
            when(patientService.getPatientById(patientId)).thenReturn(responseDTO);

            mockMvc.perform(get("/patients/{id}", patientId));

            verify(patientService).getPatientById(patientId);
        }
    }

    @Nested
    @DisplayName("POST /patients")
    class CreatePatientTests {

        @Test
        @DisplayName("returns 200 with created patient on valid request")
        void createPatient_validRequest_returnsOk() throws Exception {
            when(patientService.createPatient(any(PatientRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.id").value(patientId.toString()));
        }

        @Test
        @DisplayName("returns 400 when name is blank")
        void createPatient_blankName_returns400() throws Exception {
            validCreateRequest.setName("");

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("returns 400 when name exceeds 100 characters")
        void createPatient_nameTooLong_returns400() throws Exception {
            validCreateRequest.setName("A".repeat(101));

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("returns 400 when email format is invalid")
        void createPatient_invalidEmail_returns400() throws Exception {
            validCreateRequest.setEmail("not-an-email");

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void createPatient_blankEmail_returns400() throws Exception {
            validCreateRequest.setEmail("");

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("returns 400 when address is blank")
        void createPatient_blankAddress_returns400() throws Exception {
            validCreateRequest.setAddress("");

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("returns 400 when dateOfBirth is blank")
        void createPatient_blankDateOfBirth_returns400() throws Exception {
            validCreateRequest.setDateOfBirth("");

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("returns 400 when registeredDate is missing — CreatePatientValidationGroup")
        void createPatient_missingRegisteredDate_returns400() throws Exception {
            validCreateRequest.setRegisteredDate(null);

            mockMvc.perform(post("/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }
    }

    @Nested
    @DisplayName("PUT /patients/{id}")
    class UpdatePatientTests {

        @Test
        @DisplayName("returns 200 with updated patient on valid request")
        void updatePatient_validRequest_returnsOk() throws Exception {
            // registeredDate is NOT required on PUT (only Default.class validation group applied)
            PatientRequestDTO updateRequest = new PatientRequestDTO();
            updateRequest.setName("John Updated");
            updateRequest.setEmail("john.updated@example.com");
            updateRequest.setAddress("789 New St");
            updateRequest.setDateOfBirth("1990-06-15");

            PatientResponseDTO updatedResponse = new PatientResponseDTO();
            updatedResponse.setId(patientId.toString());
            updatedResponse.setName("John Updated");
            updatedResponse.setEmail("john.updated@example.com");
            updatedResponse.setAddress("789 New St");
            updatedResponse.setDateOfBirth("1990-06-15");

            when(patientService.updatePatient(eq(patientId), any(PatientRequestDTO.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/patients/{id}", patientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("John Updated"))
                    .andExpect(jsonPath("$.email").value("john.updated@example.com"));
        }

        @Test
        @DisplayName("registeredDate is optional on PUT — request without it passes validation")
        void updatePatient_withoutRegisteredDate_passesValidation() throws Exception {
            PatientRequestDTO updateRequest = new PatientRequestDTO();
            updateRequest.setName("John");
            updateRequest.setEmail("john@example.com");
            updateRequest.setAddress("123 St");
            updateRequest.setDateOfBirth("1990-06-15");
            // registeredDate intentionally absent

            when(patientService.updatePatient(eq(patientId), any(PatientRequestDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(put("/patients/{id}", patientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 400 when name is blank on update")
        void updatePatient_blankName_returns400() throws Exception {
            validCreateRequest.setName("");

            mockMvc.perform(put("/patients/{id}", patientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(patientService);
        }

        @Test
        @DisplayName("passes the correct UUID and DTO to the service")
        void updatePatient_passesCorrectArgsToService() throws Exception {
            when(patientService.updatePatient(eq(patientId), any(PatientRequestDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(put("/patients/{id}", patientId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateRequest)));

            verify(patientService).updatePatient(eq(patientId), any(PatientRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /patients/{id}")
    class DeletePatientTests {

        @Test
        @DisplayName("returns 204 No Content on successful delete")
        void deletePatient_validId_returns204() throws Exception {
            doNothing().when(patientService).deletePatient(patientId);

            mockMvc.perform(delete("/patients/{id}", patientId))
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("calls service deletePatient with the correct UUID")
        void deletePatient_callsServiceWithCorrectId() throws Exception {
            mockMvc.perform(delete("/patients/{id}", patientId))
                    .andExpect(status().isNoContent());

            verify(patientService, times(1)).deletePatient(patientId);
        }
    }
}