package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.kafkaProducer;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Pure Mockito — no Spring context, no @MockBean needed here.
// @Mock and @InjectMocks are from Mockito itself, not Spring Boot.
// No deprecation concerns in this file.
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private BillingServiceGrpcClient billingServiceGrpcClient;
    @Mock private kafkaProducer kafkaProducer;

    @InjectMocks private PatientService patientService;

    private Patient patient;
    private PatientRequestDTO requestDTO;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(patientId);
        patient.setName("John Doe");
        patient.setEmail("john@example.com");
        patient.setAddress("123 Main St");
        patient.setdateOfBirth(LocalDate.of(1990, 6, 15));
        patient.setRegisteredDate(LocalDate.of(2024, 1, 10));

        requestDTO = new PatientRequestDTO();
        requestDTO.setName("John Doe");
        requestDTO.setEmail("john@example.com");
        requestDTO.setAddress("123 Main St");
        requestDTO.setDateOfBirth("1990-06-15");
        requestDTO.setRegisteredDate("2024-01-10");
    }

    @Nested
    @DisplayName("getPatients()")
    class GetPatientsTests {

        @Test
        @DisplayName("returns list of all patients mapped to DTOs")
        void getPatients_multiplePatients_returnsDTOList() {
            Patient second = new Patient();
            second.setId(UUID.randomUUID());
            second.setName("Jane Doe");
            second.setEmail("jane@example.com");
            second.setAddress("456 Other St");
            second.setdateOfBirth(LocalDate.of(1995, 3, 20));
            second.setRegisteredDate(LocalDate.of(2024, 2, 1));

            when(patientRepository.findAll()).thenReturn(List.of(patient, second));

            List<PatientResponseDTO> result = patientService.getPatients();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmail()).isEqualTo("john@example.com");
            assertThat(result.get(1).getEmail()).isEqualTo("jane@example.com");
            verify(patientRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("returns empty list when no patients exist")
        void getPatients_noPatients_returnsEmptyList() {
            when(patientRepository.findAll()).thenReturn(Collections.emptyList());

            List<PatientResponseDTO> result = patientService.getPatients();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPatientById()")
    class GetPatientByIdTests {

        @Test
        @DisplayName("returns correct DTO when patient exists")
        void getPatientById_exists_returnsDTO() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

            PatientResponseDTO result = patientService.getPatientById(patientId);

            assertThat(result.getId()).isEqualTo(patientId.toString());
            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("throws PatientNotFoundException when patient does not exist")
        void getPatientById_notFound_throwsPatientNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(patientRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.getPatientById(unknownId))
                    .isInstanceOf(PatientNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("createPatient()")
    class CreatePatientTests {

        @Test
        @DisplayName("saves patient and returns DTO on success")
        void createPatient_validRequest_savesAndReturnsDTO() {
            when(patientRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);

            PatientResponseDTO result = patientService.createPatient(requestDTO);

            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getName()).isEqualTo("John Doe");
            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("calls billing gRPC and Kafka after saving")
        void createPatient_success_triggersGrpcAndKafka() {
            when(patientRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);

            patientService.createPatient(requestDTO);

            verify(billingServiceGrpcClient).createBillingAccount(
                    eq(patientId.toString()),
                    eq("John Doe"),
                    eq("john@example.com")
            );
            verify(kafkaProducer).sendEvent(patient);
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException and never saves when email is duplicate")
        void createPatient_duplicateEmail_throwsAndDoesNotSave() {
            when(patientRepository.existsByEmail(requestDTO.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> patientService.createPatient(requestDTO))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(requestDTO.getEmail());

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("gracefully continues when gRPC billing call fails")
        void createPatient_billingServiceFails_stillReturnsDTO() {
            when(patientRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);
            doThrow(new RuntimeException("gRPC connection refused"))
                    .when(billingServiceGrpcClient)
                    .createBillingAccount(anyString(), anyString(), anyString());

            PatientResponseDTO result = patientService.createPatient(requestDTO);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("gracefully continues when Kafka event publish fails")
        void createPatient_kafkaFails_stillReturnsDTO() {
            when(patientRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);
            doThrow(new RuntimeException("Kafka broker unavailable"))
                    .when(kafkaProducer).sendEvent(any(Patient.class));

            PatientResponseDTO result = patientService.createPatient(requestDTO);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("updatePatient()")
    class UpdatePatientTests {

        @Test
        @DisplayName("updates all mutable fields and returns updated DTO")
        void updatePatient_validRequest_updatesAndReturnsDTO() {
            PatientRequestDTO updateRequest = new PatientRequestDTO();
            updateRequest.setName("John Updated");
            updateRequest.setEmail("john.updated@example.com");
            updateRequest.setAddress("789 New St");
            updateRequest.setDateOfBirth("1990-06-15");

            Patient updatedPatient = new Patient();
            updatedPatient.setId(patientId);
            updatedPatient.setName("John Updated");
            updatedPatient.setEmail("john.updated@example.com");
            updatedPatient.setAddress("789 New St");
            updatedPatient.setdateOfBirth(LocalDate.of(1990, 6, 15));

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientRepository.existsByEmailAndIdNot("john.updated@example.com", patientId)).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(updatedPatient);

            PatientResponseDTO result = patientService.updatePatient(patientId, updateRequest);

            assertThat(result.getName()).isEqualTo("John Updated");
            assertThat(result.getEmail()).isEqualTo("john.updated@example.com");
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("throws PatientNotFoundException when patient does not exist")
        void updatePatient_notFound_throwsPatientNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(patientRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.updatePatient(unknownId, requestDTO))
                    .isInstanceOf(PatientNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email belongs to another patient")
        void updatePatient_emailTakenByAnotherPatient_throwsEmailAlreadyExistsException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientRepository.existsByEmailAndIdNot(requestDTO.getEmail(), patientId)).thenReturn(true);

            assertThatThrownBy(() -> patientService.updatePatient(patientId, requestDTO))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(requestDTO.getEmail());

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows patient to update with their own current email")
        void updatePatient_sameEmail_doesNotThrow() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientRepository.existsByEmailAndIdNot(requestDTO.getEmail(), patientId)).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);

            PatientResponseDTO result = patientService.updatePatient(patientId, requestDTO);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("deletePatient()")
    class DeletePatientTests {

        @Test
        @DisplayName("calls deleteById with the correct UUID")
        void deletePatient_validId_callsDeleteById() {
            patientService.deletePatient(patientId);

            verify(patientRepository, times(1)).deleteById(patientId);
        }

        @Test
        @DisplayName("does not interact with gRPC or Kafka on delete")
        void deletePatient_noSideEffects() {
            patientService.deletePatient(patientId);

            verifyNoInteractions(billingServiceGrpcClient);
            verifyNoInteractions(kafkaProducer);
        }
    }
}