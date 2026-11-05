"use client";
import { useState } from "react";

function PatientRegistration() {

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [submittedPatient, setSubmittedPatient] = useState(null);
  const [firstNameError, setFirstNameError] = useState("");
  const [lastNameError, setLastNameError] = useState("");

  function handleSubmit(event) {
    event.preventDefault();

    if (firstName.trim() === "") {
  setFirstNameError("First Name is required");
} else {
  setFirstNameError("");
}

    if (lastName.trim() === "") {
  setLastNameError("Last Name is required");
} else {
  setLastNameError("");
}

   if( firstName.trim() === ""||lastName.trim() === "" ){
    return;
   }

    setSubmittedPatient({
  firstName,
  lastName,
  email,
  phone
});
    console.log(
      firstName,
      lastName,
      email,
      phone
    );
  }
  return (
    <>
   <form onSubmit={handleSubmit}>
      <h1>Patient Registration</h1>
      <p>Register a new patient here.</p>

      <label htmlFor="firstName">First Name</label>
      <input
      type="text"
      id="firstName"
      name="firstName"
      value={firstName}
      onChange={(event) => {setFirstName(event.target.value); setFirstNameError("");}}
      />
      {firstNameError && <p>{firstNameError}</p>}

      <label>Last Name</label>
      <input
      type="text"
      id="lastName"
      name="lastName"
      value={lastName}
      onChange={(event) => {setLastName(event.target.value); setLastNameError("");}}
      />
      {lastNameError && <p>{lastNameError}</p>}


      <label>Email</label>
      <input 
      type="email"
      id="email"
      name="email"
      value={email}
      onChange={(event) => setEmail(event.target.value)}   
      required  
      />

      <label>Phone</label>
      <input
      type="tel"
      id="phone"
      name="phone"
      value={phone}
      onChange={(event) => setPhone(event.target.value)}
      />
      <p>First name: {firstName}</p>
      <button type="submit" >Register Patient</button>

   </form> 
   {submittedPatient && (
  <div>
    <h2>Submitted Patient Information</h2>

    <p>First Name: {submittedPatient.firstName}</p>
    <p>Last Name: {submittedPatient.lastName}</p>
    <p>Email: {submittedPatient.email}</p>
    <p>Phone: {submittedPatient.phone}</p>
  </div>
)}
   </>
  );
}



export default PatientRegistration;