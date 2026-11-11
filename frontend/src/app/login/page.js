
"use client";

import { useState } from "react";
import { apiRequest } from "@/lib/patientAPI";

export default function LoginPage() {
  // Login form state
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // Login status state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [loggedIn, setLoggedIn] = useState(false);

  // Protected API test state
  const [patientResult, setPatientResult] = useState(null);
  const [patientError, setPatientError] = useState("");
  const [testingPatients, setTestingPatients] = useState(false);

  // Handle login
  async function handleSubmit(event) {
    event.preventDefault();

    setLoading(true);
    setError("");

    try {
      const response = await apiRequest(
        "http://localhost:4004/auth/login",
        {
          method: "POST",
          body: { email, password }
        }
      );

      if (!response?.token) {
        throw new Error("Login response did not contain a token");
      }

      // Store JWT in session storage
      sessionStorage.setItem("token", response.token);

      setLoggedIn(true);
      setPassword("");
    } catch (err) {
      setError(err.message || "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  // Test protected patient API
  async function handleTestProtectedApi() {
    setTestingPatients(true);
    setPatientError("");
    setPatientResult(null);

    try {
      // Retrieve JWT from session storage
      const token = sessionStorage.getItem("token");

      if (!token) {
        throw new Error("JWT not found in session storage");
      }

      // Send JWT through the existing API helper
      const result = await apiRequest(
        "http://localhost:4004/api/patients",
        { token }
      );

      setPatientResult(result);
    } catch (err) {
      setPatientError(
        err.message || "Failed to fetch patients"
      );
    } finally {
      setTestingPatients(false);
    }
  }

  // Handle logout
  function handleLogout() {
    sessionStorage.removeItem("token");

    setLoggedIn(false);
    setEmail("");
    setPassword("");
    setError("");

    setPatientResult(null);
    setPatientError("");
  }

  return (
    <main>
      <h1>Login</h1>

      {loggedIn ? (
        <section>
          <h2>Login successful!</h2>

          <button
            type="button"
            onClick={handleTestProtectedApi}
            disabled={testingPatients}
          >
            {testingPatients
              ? "Fetching patients..."
              : "Test Protected Patient API"}
          </button>

          {patientError && (
            <p role="alert">
              {patientError}
            </p>
          )}

          {patientResult !== null && (
            <section>
              <h3>Patient API Response</h3>

              <pre>
                {JSON.stringify(patientResult, null, 2)}
              </pre>
            </section>
          )}

          <button
            type="button"
            onClick={handleLogout}
          >
            Logout
          </button>
        </section>
      ) : (
        <form onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email">Email</label>

            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) =>
                setEmail(event.target.value)
              }
              required
            />
          </div>

          <div>
            <label htmlFor="password">Password</label>

            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
          >
            {loading ? "Logging in..." : "Login"}
          </button>

          {error && (
            <p role="alert">
              {error}
            </p>
          )}
        </form>
      )}
    </main>
  );
}
