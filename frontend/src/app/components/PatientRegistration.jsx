
"use client";

import { useEffect, useState } from "react";
import { apiRequest } from "@/lib/patientAPI";

function PatientRegistration({ onPatientSaved, selectedPatient }) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [registeredDate, setRegisteredDate] = useState("");

  useEffect(() => {
  if (selectedPatient) {
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
    setErrors({});
    setErrorMessage("");
  }
}, [selectedPatient]);

  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();

    setErrors({});
    setSuccessMessage("");
    setErrorMessage("");

    // Validate required fields
    const newErrors = {};

    if (!name.trim()) newErrors.name = "Name is required";
    if (!email.trim()) newErrors.email = "Email is required";
    if (!address.trim()) newErrors.address = "Address is required";
    if (!dateOfBirth) newErrors.dateOfBirth = "Date of birth is required";
    if (!selectedPatient &&!registeredDate) newErrors.registeredDate = "Registered date is required";

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // Get authentication token
    const token = sessionStorage.getItem("token");

    if (!token) {
      setErrorMessage("Please log in before registering a patient.");
      return;
    }

    // Prepare backend request data
    const patientData = {
      name: name.trim(),
      email: email.trim(),
      address: address.trim(),
      dateOfBirth,
      ...(!selectedPatient&&{registeredDate})
    };

    const url = selectedPatient
                ?`http://localhost:4004/api/patients/${selectedPatient.id}`
                :"http://localhost:4004/api/patients";

    const method = selectedPatient ? "PUT" : "POST";
    
    
    try {
      setSubmitting(true);

      await apiRequest(url,
        {
          method,
          token,
          body: patientData
        }
      );

      setSuccessMessage(
        selectedPatient 
        ?"Patient updated successfully!"
        :"Patient registered successfully!"
      );

      // Clear form after successful creation
      setName("");
      setEmail("");
      setAddress("");
      setDateOfBirth("");
      setRegisteredDate("");

      // Notify parent to refresh patient list
      onPatientSaved?.();

    } catch (error) {
      setErrorMessage(
        error.message || "Unable to register patient."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>{selectedPatient ? "Edit Patient" : "Register New Patient"}</h2>

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
      {errors.name && <p>{errors.name}</p>}

      <label htmlFor="email">Email</label>
      <input
        id="email"
        type="email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        required
      />
      {errors.email && <p>{errors.email}</p>}

      <label htmlFor="address">Address</label>
      <input
        id="address"
        type="text"
        value={address}
        onChange={(event) => setAddress(event.target.value)}
        required
      />
      {errors.address && <p>{errors.address}</p>}

      <label htmlFor="dateOfBirth">Date of Birth</label>
      <input
        id="dateOfBirth"
        type="date"
        value={dateOfBirth}
        onChange={(event) => setDateOfBirth(event.target.value)}
        required
      />
      {errors.dateOfBirth && <p>{errors.dateOfBirth}</p>}

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
    {errors.registeredDate && <p>{errors.registeredDate}</p>}
  </>
)}

<button type="submit" disabled={submitting}>
  {submitting
    ? selectedPatient ? "Updating..." : "Registering..."
    : selectedPatient ? "Update Patient" : "Register Patient"}
</button>
    </form>
  );
}

export default PatientRegistration;
