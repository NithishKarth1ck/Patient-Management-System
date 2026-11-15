
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
    patient.name.toLowerCase().includes(normalizedSearch) ||
    patient.email.toLowerCase().includes(normalizedSearch)
  );

  // Fetch patients from backend
  const fetchPatients = useCallback(async (signal) => {
    const token = sessionStorage.getItem("token");

    if (!token) {
      router.replace("/login");
      return;
    }

    setLoading(true);
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
      if (!signal?.aborted) {
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
    fetchPatients();
  }

  // Called when user clicks Edit
  function handleEdit(patient) {
    setSelectedPatient(patient);
  }

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

  try {
    await apiRequest(
      `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/patients/${patient.id}`,
      {
        method: "DELETE",
        token
      }
    );

    await fetchPatients();

  } catch (error) {
    setError(error.message || "Unable to delete patient.");
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

  if (loading) {
    return <p>Loading patients...</p>;
  }

  if (error) {
    return (
      <main>
        <h2>Unable to load patients</h2>
        <p>{error}</p>

        <button onClick={() => fetchPatients()}>
          Try Again
        </button>
      </main>
    );
  }

  return (
    <main>
      <header>
        <h1>Patient Management System</h1>

        <button onClick={handleLogout}>
          Logout
        </button>
      </header>

      <section>
        <h2>Patients</h2>

        <input
          type="text"
          placeholder="Search by name or email"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />

        <PatientRegistration
          selectedPatient={selectedPatient}
          onPatientSaved={handlePatientSaved}
          onCancelEdit={handleCancelEdit}
        />

        {patients.length === 0 ? (
          <p>No patients found.</p>
        ) : filteredPatients.length === 0 ? (
          <p>No matching patients.</p>
        ) : (
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
        )}
      </section>
    </main>
  );
}
