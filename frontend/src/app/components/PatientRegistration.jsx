
"use client";

import { useEffect, useState } from "react";
import { apiRequest } from "@/lib/patientAPI";

function PatientRegistration({
  onPatientSaved,
  onCancelEdit,
  selectedPatient
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [registeredDate, setRegisteredDate] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  // Populate the form when editing.
  // Reset it when returning to creation mode.
useEffect(() => {
  setErrorMessage("");

  if (selectedPatient) {
    setSuccessMessage("");
    setName(selectedPatient.name);
    setEmail(selectedPatient.email);
    setAddress(selectedPatient.address);
    setDateOfBirth(selectedPatient.dateOfBirth);
  } else {
    setName("");
    setEmail("");
    setAddress("");
    setDateOfBirth("");
    setRegisteredDate("");
  }
}, [selectedPatient]);

  async function handleSubmit(event) {
    event.preventDefault();

    setSuccessMessage("");
    setErrorMessage("");

    // Get authentication token
    const token = sessionStorage.getItem("token");

    if (!token) {
      setErrorMessage("Please log in before registering a patient.");
      return;
    }

    // Prepare request data
    const patientData = {
      name: name.trim(),
      email: email.trim(),
      address: address.trim(),
      dateOfBirth,
      ...(!selectedPatient && { registeredDate })
    };

    // Select URL and HTTP method based on the operation
const url = selectedPatient
  ? `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/patients/${selectedPatient.id}`
  : `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/patients`;

    const method = selectedPatient ? "PUT" : "POST";

    try {
      setSubmitting(true);

      await apiRequest(url, {
        method,
        token,
        body: patientData
      });

      setSuccessMessage(
        selectedPatient
          ? "Patient updated successfully!"
          : "Patient registered successfully!"
      );

      // Notify the parent after a successful request
      onPatientSaved?.();

    } catch (error) {
      setErrorMessage(
        error.message ||
          (selectedPatient
            ? "Unable to update patient."
            : "Unable to register patient.")
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>
        {selectedPatient
          ? "Edit Patient"
          : "Register New Patient"}
      </h2>

      {successMessage && <p>{successMessage}</p>}
      {errorMessage && <p>{errorMessage}</p>}

      <label htmlFor="name">Name</label>
      <input
        id="name"
        type="text"
        value={name}
        onChange={(event) => setName(event.target.value)}
        required
      />

      <label htmlFor="email">Email</label>
      <input
        id="email"
        type="email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        required
      />

      <label htmlFor="address">Address</label>
      <input
        id="address"
        type="text"
        value={address}
        onChange={(event) => setAddress(event.target.value)}
        required
      />

      <label htmlFor="dateOfBirth">Date of Birth</label>
      <input
        id="dateOfBirth"
        type="date"
        value={dateOfBirth}
        onChange={(event) => setDateOfBirth(event.target.value)}
        required
      />

      {!selectedPatient && (
        <>
          <label htmlFor="registeredDate">Registered Date</label>
          <input
            id="registeredDate"
            type="date"
            value={registeredDate}
            onChange={(event) => setRegisteredDate(event.target.value)}
            required
          />
        </>
      )}

      <button type="submit" disabled={submitting}>
        {submitting
          ? selectedPatient
            ? "Updating..."
            : "Registering..."
          : selectedPatient
            ? "Update Patient"
            : "Register Patient"}
      </button>

      {selectedPatient && (
        <button
          type="button"
          onClick={onCancelEdit}
          disabled={submitting}
        >
          Cancel Edit
        </button>
      )}
    </form>
  );
}

export default PatientRegistration;
