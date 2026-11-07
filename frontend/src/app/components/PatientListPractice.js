"use client";

import { useState, useEffect } from "react";
import { getPatients } from "../../lib/patientAPI";

export default function PatientList({ url }) {
  const [patients, setPatients] = useState([]);
  const [error, setError] = useState(null);
  const [result, setResult] = useState({
  url: null,
  patients: [],
  error: null
});

  const loading = result.url !== url;

  useEffect(() => {
  const controller = new AbortController();

  getPatients(url, controller.signal)
    .then(data => {
      if (!controller.signal.aborted) {
        setResult({
          url,
          patients: data,
          error: null
        });
      }
    })
    .catch(error => {
      if (!controller.signal.aborted && error.name !== "AbortError") {
        setResult({
          url,
          patients: [],
          error: error.message
        });
      }
    });

  return () => controller.abort();
}, [url]);

  if (loading) return <p>Loading patients...</p>;
  if (result.error) return <p>Error: {result.error}</p>;
  if (result.patients.length === 0) return <p>No patients found.</p>;

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