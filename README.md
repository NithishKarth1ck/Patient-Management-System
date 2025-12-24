🏥 Patient Management System – Microservices Architecture

A production-style backend system built using Java Spring Boot Microservices, implementing gRPC for synchronous inter-service communication and Apache Kafka for asynchronous, event-driven messaging.

🚀 Project Overview

The Patient Management System simulates a hospital backend where multiple independent services interact using REST, gRPC, and Kafka, coordinated through an API Gateway.

Microservices

API Gateway

Auth Service

Patient Service

Billing Service

Analytics Service

🧱 System Architecture
Client
  ↓
API Gateway
  ↓
-----------------------------------------------
| Auth | Patient Service | Billing | Analytics |
-----------------------------------------------
          |                 ↑
          |               gRPC
          ↓
        Kafka

🏗 Architecture Highlights

JWT-based authentication

gRPC server in Billing Service

gRPC client in Patient Service

Kafka-based event-driven communication

Patient Service → Kafka Producer

Analytics Service → Kafka Consumer

Dockerized microservices

Cloud-ready (AWS)

🛠 Tech Stack

Java 17

Spring Boot

Spring Security + JWT

Spring Data JPA

REST APIs

gRPC (Protocol Buffers)

Apache Kafka

Docker & Docker Compose

Maven

MySQL / PostgreSQL

✨ Key Features

Secure authentication & authorization

Microservices-based architecture

gRPC synchronous communication (Patient → Billing)

Kafka asynchronous event processing

Centralized API Gateway

Containerized services

📡 Inter-Service Communication
gRPC

Billing Service acts as gRPC server

Patient Service acts as gRPC client

Used for low-latency synchronous communication

Kafka

Patient Service publishes events (Producer)

Analytics Service consumes events (Consumer)

Used for asynchronous, decoupled processing

▶ How to Run Locally
Prerequisites

Java 17+

Maven

Docker & Docker Compose

Steps
git clone https://github.com/NithishKarth1ck/Patient-Management-System.git
cd Patient-Management-System
docker compose up --build

📑 API Documentation

REST APIs:

http://localhost:<port>/swagger-ui.html


gRPC contracts defined using .proto files

🧠 What I Learned

Microservices design using Spring Boot

Implementing gRPC server and client

Event-driven architecture using Kafka

Choosing synchronous vs asynchronous communication

Dockerizing distributed systems

📈 Future Improvements

Kafka retry & dead-letter topics

gRPC contract testing

Distributed tracing

CI/CD pipeline

👨‍💻 Author

Nithish Karthick
Java Backend Developer | Spring Boot | Microservices | Kafka | gRPC

GitHub: https://github.com/NithishKarth1ck

LinkedIn: (add your LinkedIn link)

⭐ Note

This repository demonstrates real-world backend communication patterns using Kafka and gRPC and is intended for learning and demonstration purposes.
