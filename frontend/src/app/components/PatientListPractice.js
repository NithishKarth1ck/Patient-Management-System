"use client";

import { useState, useEffect } from "react";
import { getPatients } from "./patientAPI";

export default function PatientList({ url }) {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError(null);

    getPatients(url, controller.signal)
      .then(data => {
        if (!controller.signal.aborted) {
          setPatients(data);
        }
      })
      .catch(error => {
        if (error.name === "AbortError") return;

        setError(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [url]);

  if (loading) return <p>Loading patients...</p>;
  if (error) return <p>Error: {error}</p>;
  if (patients.length === 0) return <p>No patients found.</p>;

  return (
    <div>
      <h2>Patient List</h2>
      <ul>
        {patients.map(patient => (
          <li key={patient.id}>{patient.name}</li>
        ))}
      </ul>
    </div>
  );
}