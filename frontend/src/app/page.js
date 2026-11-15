
"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiRequest } from "@/lib/patientAPI";
import PatientRegistration from "@/app/components/PatientRegistration";

export default function Home() {
  const router = useRouter();

  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedPatient, setSelectedPatient] = useState(null);

  // Prepare search query
  const normalizedSearch = searchTerm.trim().toLowerCase();

  // Filter patients based on name or email
  const filteredPatients = patients.filter((patient) =>
    (patient.name || "").toLowerCase().includes(normalizedSearch) ||
    (patient.email || "").toLowerCase().includes(normalizedSearch)
  );

  // Fetch patients from backend
  const fetchPatients = useCallback(async (signal, showLoading = true) => {
    const token = sessionStorage.getItem("token");

    if (!token) {
      router.replace("/login");
      return;
    }

    if (showLoading) {
      setLoading(true);
    }

    setError("");

    try {
      const data = await apiRequest(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/patients`,
        {
          method: "GET",
          token,
          signal
        }
      );

      if (!Array.isArray(data)) {
        throw new Error("Unexpected patient data received.");
      }

      setPatients(data);

    } catch (err) {
      if (err.name !== "AbortError") {
        setError(err.message || "Failed to fetch patients.");
      }
    } finally {
      if (showLoading && !signal?.aborted) {
        setLoading(false);
      }
    }
  }, [router]);

  // Fetch patients when dashboard mounts
  useEffect(() => {
    const controller = new AbortController();

    fetchPatients(controller.signal);

    return () => controller.abort();
  }, [fetchPatients]);

  // Called after successful patient creation or update
  function handlePatientSaved() {
    setSelectedPatient(null);
    fetchPatients(undefined, false);
  }

  // Called when user clicks Edit
  function handleEdit(patient) {
    setSelectedPatient(patient);
  }

  // Delete a patient
  async function handleDelete(patient) {
    const confirmed = window.confirm(
      `Are you sure you want to delete ${patient.name}?`
    );

    if (!confirmed) {
      return;
    }

    const token = sessionStorage.getItem("token");

    if (!token) {
      router.replace("/login");
      return;
    }

    setError("");

    try {
      await apiRequest(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/patients/${patient.id}`,
        {
          method: "DELETE",
          token
        }
      );

      if (selectedPatient?.id === patient.id) {
        setSelectedPatient(null);
      }

      await fetchPatients(undefined, false);

    } catch (err) {
      setError(err.message || "Unable to delete patient.");
    }
  }

  // Called when user cancels editing
  function handleCancelEdit() {
    setSelectedPatient(null);
  }

  // Logout
  function handleLogout() {
    sessionStorage.removeItem("token");
    router.replace("/login");
  }

  // Initial loading state
  if (loading) {
    return <p className="message">Loading patients...</p>;
  }

  // Show full-page error if patient data could not be loaded
  if (error && patients.length === 0) {
    return (
      <main className="dashboard">
        <h2>Unable to load patients</h2>
        <p className="error">{error}</p>

        <button onClick={() => fetchPatients()}>
          Try Again
        </button>
      </main>
    );
  }

  return (
    <main className="dashboard">

      <header className="dashboard-header">
        <h1>Patient Management System</h1>

        <button onClick={handleLogout}>
          Logout
        </button>
      </header>

      {/* Patient registration form */}
      <section className="panel">
        <h2>
          {selectedPatient ? "Edit Patient" : "Register Patient"}
        </h2>

        <PatientRegistration
          selectedPatient={selectedPatient}
          onPatientSaved={handlePatientSaved}
          onCancelEdit={handleCancelEdit}
        />
      </section>

      {/* Patient list */}
      <section className="panel">
        <h2>Patients</h2>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <input
          className="search-input"
          type="text"
          placeholder="Search by name or email"
          aria-label="Search patients by name or email"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />

        {patients.length === 0 ? (
          <p className="message">No patients found.</p>
        ) : filteredPatients.length === 0 ? (
          <p className="message">No matching patients.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Address</th>
                  <th>Date of Birth</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {filteredPatients.map((patient) => (
                  <tr key={patient.id}>
                    <td>{patient.name}</td>
                    <td>{patient.email}</td>
                    <td>{patient.address}</td>
                    <td>{patient.dateOfBirth}</td>
                    <td>
                      <button onClick={() => handleEdit(patient)}>
                        Edit
                      </button>

                      <button onClick={() => handleDelete(patient)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

    </main>
  );
}
