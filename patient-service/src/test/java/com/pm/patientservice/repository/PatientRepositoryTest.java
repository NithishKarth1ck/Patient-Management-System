package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// No @MockBean / @MockitoBean used here — no Spring Boot web context either.
// @DataJpaTest loads only JPA: entities, repositories, Hibernate.
// @AutoConfigureTestDatabase(replace = NONE) prevents Spring from swapping
// PostgreSQL with H2. Always use NONE when you have Testcontainers.
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PatientRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("patient_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private PatientRepository patientRepository;

    private Patient patientA;
    private Patient patientB;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();

        patientA = new Patient();
        patientA.setName("Alice");
        patientA.setEmail("alice@example.com");
        patientA.setAddress("1 Alice Road");
        patientA.setdateOfBirth(LocalDate.of(1990, 1, 1));
        patientA.setRegisteredDate(LocalDate.of(2024, 1, 1));

        patientB = new Patient();
        patientB.setName("Bob");
        patientB.setEmail("bob@example.com");
        patientB.setAddress("2 Bob Street");
        patientB.setdateOfBirth(LocalDate.of(1985, 5, 20));
        patientB.setRegisteredDate(LocalDate.of(2024, 2, 1));

        patientA = patientRepository.save(patientA);
        patientB = patientRepository.save(patientB);
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns all saved patients")
        void findAll_returnsBothPatients() {
            List<Patient> patients = patientRepository.findAll();
            assertThat(patients).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when table is empty")
        void findAll_afterDeleteAll_returnsEmpty() {
            patientRepository.deleteAll();
            assertThat(patientRepository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns patient when ID exists")
        void findById_existingId_returnsPatient() {
            Optional<Patient> result = patientRepository.findById(patientA.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("alice@example.com");
            assertThat(result.get().getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("returns empty Optional when ID does not exist")
        void findById_unknownId_returnsEmpty() {
            Optional<Patient> result = patientRepository.findById(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmailTests {

        @Test
        @DisplayName("returns true when email exists in DB")
        void existsByEmail_emailExists_returnsTrue() {
            assertThat(patientRepository.existsByEmail("alice@example.com")).isTrue();
        }

        @Test
        @DisplayName("returns false when email does not exist in DB")
        void existsByEmail_emailNotExists_returnsFalse() {
            assertThat(patientRepository.existsByEmail("nobody@example.com")).isFalse();
        }

        @Test
        @DisplayName("is case-sensitive — different case is treated as different email")
        void existsByEmail_differentCase_returnsFalse() {
            assertThat(patientRepository.existsByEmail("ALICE@EXAMPLE.COM")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmailAndIdNot()")
    class ExistsByEmailAndIdNotTests {

        @Test
        @DisplayName("returns true when email belongs to a DIFFERENT patient — real conflict")
        void existsByEmailAndIdNot_emailBelongsToOtherPatient_returnsTrue() {
            // Bob's email, checked from Alice's perspective → conflict
            boolean result = patientRepository.existsByEmailAndIdNot(
                    "bob@example.com", patientA.getId()
            );
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when email belongs to the SAME patient — own email on update")
        void existsByEmailAndIdNot_emailBelongsToSamePatient_returnsFalse() {
            // Alice updating with her own email → should NOT be flagged as conflict
            boolean result = patientRepository.existsByEmailAndIdNot(
                    "alice@example.com", patientA.getId()
            );
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false when email does not exist at all")
        void existsByEmailAndIdNot_emailDoesNotExist_returnsFalse() {
            boolean result = patientRepository.existsByEmailAndIdNot(
                    "brand.new@example.com", patientA.getId()
            );
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("save() and deleteById()")
    class SaveAndDeleteTests {

        @Test
        @DisplayName("persists a new patient and auto-generates a UUID")
        void save_newPatient_persistsWithGeneratedId() {
            Patient newPatient = new Patient();
            newPatient.setName("Charlie");
            newPatient.setEmail("charlie@example.com");
            newPatient.setAddress("3 Charlie Lane");
            newPatient.setdateOfBirth(LocalDate.of(2000, 12, 31));
            newPatient.setRegisteredDate(LocalDate.of(2024, 3, 1));

            Patient saved = patientRepository.save(newPatient);

            assertThat(saved.getId()).isNotNull();
            assertThat(patientRepository.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("deleteById removes only the target patient")
        void deleteById_removesOnlyTargetPatient() {
            patientRepository.deleteById(patientA.getId());

            assertThat(patientRepository.findAll()).hasSize(1);
            assertThat(patientRepository.findById(patientA.getId())).isEmpty();
            assertThat(patientRepository.findById(patientB.getId())).isPresent();
        }
    }
}