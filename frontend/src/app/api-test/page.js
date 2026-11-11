
"use client";

import { useState } from "react";
import { apiRequest } from "../../lib/patientAPI";

export default function ApiTestPage() {
  const [result, setResult] = useState("");

  async function testRequest() {
    try {
      const token = prompt("Paste your JWT token");

      const data = await apiRequest(
        "http://localhost:4004/api/patients",
        { token }
      );

      setResult(JSON.stringify(data, null, 2));
    } catch (error) {
      setResult(error.message);
    }
  }

  return (
    <main>
      <h1>API Helper Test</h1>
      <button onClick={testRequest}>
        Test Patient API
      </button>

      <pre>{result}</pre>
    </main>
  );
}
