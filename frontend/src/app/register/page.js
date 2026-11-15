
"use client";

import { useState } from "react";
import Link from "next/link";
import { apiRequest } from "@/lib/patientAPI";

export default function RegisterPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();

    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const response = await apiRequest(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/auth/register`,
        {
          method: "POST",
          body: {
            name: name.trim(),
            email: email.trim(),
            password,
          },
        }
      );

      setSuccess(
        response?.message || "Account created successfully. Please log in."
      );

      setName("");
      setEmail("");
      setPassword("");
    } catch (err) {
      setError(err.message || "Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-container">
      <div className="auth-form">    
      <h1>Create an Account</h1>

      <form className="auth-fields" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="name">Name</label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? "Creating Account..." : "Register"}
        </button>

        {error && <p role="alert">{error}</p>}
        {success && <p role="status">{success}</p>}
      </form>

      <p>
        Already have an account? <Link href="/login">Log in</Link>
      </p>
      </div>
    </main>
  );
}
