package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    public PatientService(
            PatientRepository patientRepository,
            EmailService emailService,
            SmsService smsService) {
        this.patientRepository = patientRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public List<PatientResponseDTO> getPatients() {
        return patientRepository.findAll().stream()
                .map(PatientMapper::toDTO)
                .toList();
    }

    public PatientResponseDTO getPatientById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(
                        "Patient not found with ID: " + id));
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO createPatient(PatientRequestDTO request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "A patient with this email already exists " + request.getEmail());
        }

        Patient patient = patientRepository.save(PatientMapper.toModel(request));

        // Notifications are best-effort and must not block patient registration.
        try {
            emailService.sendPatientRegistrationEmail(patient.getName(), patient.getEmail());
        } catch (Exception e) {
            log.warn("Registration email could not be sent for patient {}: {}",
                    patient.getId(), e.getMessage());
        }

        try {
            smsService.sendRegistrationSms(patient.getName(), patient.getPhone());
        } catch (Exception e) {
            log.warn("Registration SMS could not be sent for patient {}: {}",
                    patient.getId(), e.getMessage());
        }

        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(
                        "Patient not found with ID: " + id));

        if (patientRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new EmailAlreadyExistsException(
                    "A patient with this email already exists " + request.getEmail());
        }

        patient.setName(request.getName());
        patient.setAddress(request.getAddress());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));

        return PatientMapper.toDTO(patientRepository.save(patient));
    }

    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
        log.info("Patient deleted: {}", id);
    }
}
