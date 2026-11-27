package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @InjectMocks private PatientService patientService;

    private Patient patient;
    private PatientRequestDTO request;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        patient = new Patient();
        patient.setId(patientId);
        patient.setName("John Doe");
        patient.setEmail("john@example.com");
        patient.setAddress("123 Main St");
        patient.setPhone("9876543210");
        patient.setDateOfBirth(LocalDate.of(1990, 6, 15));

        request = new PatientRequestDTO();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setAddress("123 Main St");
        request.setPhone("9876543210");
        request.setDateOfBirth("1990-06-15");
    }

    @Test
    void getPatients_returnsMappedPatients() {
        when(patientRepository.findAll()).thenReturn(List.of(patient));
        assertThat(patientService.getPatients()).hasSize(1);
    }

    @Test
    void getPatientById_returnsPatientWhenFound() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        assertThat(patientService.getPatientById(patientId).getEmail())
                .isEqualTo("john@example.com");
    }

    @Test
    void getPatientById_throwsWhenMissing() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> patientService.getPatientById(patientId))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void createPatient_savesAndReturnsPatient() {
        when(patientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        PatientResponseDTO result = patientService.createPatient(request);

        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void createPatient_rejectsDuplicateEmail() {
        when(patientRepository.existsByEmail(request.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void updatePatient_updatesExistingPatient() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.existsByEmailAndIdNot(request.getEmail(), patientId))
                .thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        assertThat(patientService.updatePatient(patientId, request)).isNotNull();
        verify(patientRepository).save(patient);
    }

    @Test
    void deletePatient_deletesById() {
        patientService.deletePatient(patientId);
        verify(patientRepository).deleteById(patientId);
    }
}
