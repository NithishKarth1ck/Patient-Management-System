# Patient Management System - Microservices Architecture

A event-driven microservices application for patient management, built with Spring Boot and deployed on AWS ECS Fargate.

> 📸 **Screenshots:** All project screenshots are available in the [`screenshots`](./screenshots) folder.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Technologies](#technologies)
- [AWS Infrastructure](#aws-infrastructure)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Getting Started](#getting-started)
- [Deployment](#deployment)
- [Future Enhancements](#future-enhancements)

## 🎯 Overview

A scalable microservices-based patient management system demonstrating modern cloud-native architecture patterns including:
- Database per service pattern
- API Gateway with JWT authentication
- Synchronous communication via gRPC
- Asynchronous messaging with Apache Kafka
- Containerized deployment on AWS ECS Fargate

## 🏗️ Architecture

### High-Level Architecture
```
Internet → ALB → API Gateway → Auth Service → auth-db (PostgreSQL)
                             → Patient Service → patient-db (PostgreSQL)
                                              → Billing Service (gRPC)
                                              → Kafka → Analytics Service
```

## Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                         Internet                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │     ALB     │ (Public)
                    └──────┬──────┘
                           │
                  ┌────────▼────────┐
                  │  API Gateway    │ (Port 4004)
                  └────┬───────┬────┘
                       │       │
        ┌──────────────┘       └──────────────┐
        │                                     │
  ┌─────▼──────┐                       ┌──────▼──────┐
  │Auth Service│                       │   Patient   │
  │ (Port 4005)│                       │   Service   │
  └─────┬──────┘                       │ (Port 4000) │
        │                              └──────┬──────┘
  ┌─────▼──────┐                              │
  │  auth-db   │                      ┌───────┼───────┐
  │ PostgreSQL │                      │       │       │
  └────────────┘                ┌─────▼─┐  ┌──▼─── ┐ ┌▼────────┐
                                │patient│  │Billing│ │Analytics│
                                │  -db  │  │Service│ │ Service │
                                │  RDS  │  │(gRPC) │ │ (Kafka) │
                                └───────┘  └───────┘ └─────────┘
```

### Microservices

|        Service        | Port |   Type  |  Database  |           Description                |
|-----------------------|------|---------|------------|--------------------------------------|
| **API Gateway**       | 4004 | Public  |      -     | Entry point, JWT validation, routing |
| **Auth Service**      | 4005 | Private | PostgreSQL | User authentication & authorization  |
| **Patient Service**   | 4000 | Private | PostgreSQL | Patient CRUD operations              |
| **Billing Service**   | 9001 | Private |      -     | Billing account management (gRPC)    |
| **Analytics Service** | 4002 | Private |      -     | Event processing & analytics         |

### Communication Patterns

- **REST API**: Client ↔ API Gateway ↔ Services
- **gRPC**: Patient Service ↔ Billing Service (synchronous)
- **Apache Kafka**: Patient Service → Analytics Service (asynchronous)

## ✨ Features

### Functional Features
-  User registration and authentication
-  JWT-based authorization
-  Complete patient CRUD operations
-  Automated billing account creation via gRPC
-  Event-driven analytics processing

### Technical Features
-  Microservices architecture with service isolation
-  Database per service pattern
-  API Gateway pattern for centralized routing
-  Service-to-service communication (REST, gRPC, Kafka)
-  Containerization with Docker
-  Cloud deployment on AWS ECS Fargate
-  Private networking with VPC
-  Managed databases with AWS RDS
-  Service discovery with AWS Cloud Map

## 🛠️ Technologies

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.5
- **API Gateway**: Spring Cloud Gateway
- **Data Access**: Spring Data JPA
- **Security**: Spring Security with JWT
- **Communication**: 
  - REST (Spring Web)
  - gRPC (gRPC Java)
  - Kafka (Spring Kafka)

### Database
- **RDBMS**: PostgreSQL 16
- **Connection Pool**: HikariCP

### Infrastructure
- **Containerization**: Docker
- **Orchestration**: AWS ECS Fargate
- **Load Balancing**: Application Load Balancer
- **Databases**: Amazon RDS PostgreSQL
- **Networking**: Amazon VPC
- **Service Discovery**: AWS Cloud Map
- **IaC/Deployment**: AWS Copilot CLI

### Build & Tools
- **Build Tool**: Maven
- **Protocol Buffers**: For gRPC & Kafka message serialization
- **Testing**: Postman

## ☁️ AWS Infrastructure

### Deployed Components
```
VPC (10.0.0.0/16)
├── Public Subnets (2 AZs)
│   └── Application Load Balancer
├── Private Subnets (2 AZs)
│   ├── ECS Fargate Cluster
│   │   ├── api-gateway (Task)
│   │   ├── auth-service (Task)
│   │   ├── patient-service (Task)
│   │   ├── billing-service (Task)
│   │   └── analytics-service (Task)
│   └── RDS PostgreSQL Instances
│       ├── auth-db
│       └── patient-db
└── Service Discovery (AWS Cloud Map)
```

### AWS Services Used

|          Service              |              Purpose              |        Configuration           |
|-------------------------------|-----------------------------------|--------------------------------|
| **ECS Fargate**               | Container orchestration           | 0.25 vCPU, 512 MB RAM per task |
| **Application Load Balancer** | Traffic routing & SSL termination | Public-facing                  |
| **RDS PostgreSQL**            | Managed databases                 | db.t3.micro, Multi-AZ          |
| **VPC**                       | Network isolation                 | 10.0.0.0/16 CIDR               |
| **AWS Cloud Map**             | Service discovery                 | Internal DNS resolution        |
| **CloudWatch**                | Logging & monitoring              | Container logs                 |
| **ECR**                       | Container registry                | Private repositories           |

### Cost Optimization
- Used AWS Copilot for simplified deployment
- Minimal instance sizes (0.25 vCPU, 512 MB)
- No NAT Gateway (VPC Endpoints instead)
- Kafka demonstrated locally (avoided Amazon MSK costs)

## 🔌 API Endpoints

### Authentication Endpoints

| Method |     Endpoint     | Description             | Auth Required  |
|--------|------------------|-------------------------|---------------|
| POST   | `/auth/register` | Register new user       |      ❌       |
| POST   | `/auth/login`    | Login and get JWT token |      ❌       |
| GET    | `/auth/validate` | Validate JWT token      |      ✅       |

### Patient Endpoints

| Method | Endpoint             | Description        | Auth Required  |
|--------|----------------------|--------------------|----------------|
| POST   | `/api/patients`      | Create new patient |       ✅      |
| GET    | `/api/patients`      | Get all patients   |       ✅      |
| GET    | `/api/patients/{id}` | Get patient by ID  |       ✅      |
| PUT    | `/api/patients/{id}` | Update patient     |       ✅      |
| DELETE | `/api/patients/{id}` | Delete patient     |       ✅      |

### Request Examples

#### Register User
```bash
POST http://your-alb-url.amazonaws.com/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

#### Login
```bash
POST http://your-alb-url.amazonaws.com/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123"
}

Response:
{
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

#### Create Patient
```bash
POST http://your-alb-url.amazonaws.com/api/patients
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "name": "Jane Smith",
  "email": "jane@example.com",
  "address": "123 Main Street",
  "dateOfBirth": "1990-05-15",
  "registeredDate": "2026-02-14"
}
```

## 🔐 Security

### Authentication & Authorization
- **JWT-based authentication** with HS384 algorithm
- **BCrypt password hashing** (cost factor: 10)
- **Token expiration**: 10 hours
- **API Gateway validates all protected endpoints** before routing

### Network Security
- **Private subnets** for all microservices (no public IPs)
- **Security groups** restrict traffic between services
- **VPC isolation** prevents unauthorized access
- **Database encryption** at rest

### Security Flow
```
1. User registers → Password hashed with BCrypt → Stored in auth-db
2. User logs in → Credentials validated → JWT token generated
3. API request → API Gateway validates JWT → Routes to service
4. Service processes request → Returns response
```

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- Docker & Docker Compose
- AWS CLI (for deployment)
- AWS Copilot CLI (for deployment)
- Postman (for testing)

### Local Development

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/patient-management-system.git
cd patient-management-system
```

2. **Start infrastructure services**
```bash
docker-compose up -d kafka patient-db auth-db
```

3. **Run services locally**
```bash
# Terminal 1 - Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 2 - Patient Service
cd patient-service
mvn spring-boot:run

# Terminal 3 - Billing Service
cd billing-service
mvn spring-boot:run

# Terminal 4 - Analytics Service
cd analytics-service
mvn spring-boot:run

# Terminal 5 - API Gateway
cd api-gateway
mvn spring-boot:run
```

4. **Access the application**
- API Gateway: http://localhost:4004
- Test endpoints using Postman

### Environment Variables

Each service requires specific environment variables:

**auth-service**:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/authdb
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=your-base64-encoded-secret
```

**patient-service**:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/patientdb
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
BILLING_SERVICE_ADDRESS=localhost
BILLING_SERVICE_GRPC_PORT=9001
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## 📦 Deployment

### AWS Deployment with Copilot

1. **Install AWS Copilot CLI**
```bash
# macOS
brew install aws/tap/copilot-cli

# Windows
# Download from: https://github.com/aws/copilot-cli/releases
```

2. **Configure AWS credentials**
```bash
aws configure
```

3. **Initialize Copilot application**
```bash
copilot app init patient-management
copilot env init --name production --profile default
```

4. **Deploy services**
```bash
# Deploy each service
copilot svc init --name api-gateway --svc-type "Load Balanced Web Service" --dockerfile ./api-gateway/Dockerfile --port 4004
copilot svc deploy --name api-gateway --env production

copilot svc init --name auth-service --svc-type "Backend Service" --dockerfile ./auth-service/Dockerfile --port 4005
copilot svc deploy --name auth-service --env production

# Repeat for other services...
```

5. **Create RDS databases manually**
- Create `auth-db` and `patient-db` in the same VPC
- Update service manifests with database endpoints
- Redeploy services

### Docker Build
```bash
# Build all services
docker build -t patient-service ./patient-service
docker build -t auth-service ./auth-service
docker build -t billing-service ./billing-service
docker build -t analytics-service ./analytics-service
docker build -t api-gateway ./api-gateway
```

## 🎓 Key Learnings

### Microservices Patterns
- Implemented **Database per Service** pattern for data isolation
- Used **API Gateway** pattern for centralized authentication and routing
- Applied **Service Discovery** for dynamic service communication
- Demonstrated **Event-Driven Architecture** with Kafka

### Cloud Native
- Deployed containerized applications on **AWS ECS Fargate**
- Configured **private networking** with VPC and security groups
- Used **managed services** (RDS, ALB, Cloud Map) for operational efficiency
- Implemented **infrastructure as code** with AWS Copilot

### Inter-Service Communication
- **Synchronous**: REST APIs for client-facing operations
- **Synchronous**: gRPC for high-performance service-to-service calls
- **Asynchronous**: Kafka for event streaming and decoupling

### Security Best Practices
- JWT-based stateless authentication
- BCrypt password hashing
- Private subnets for backend services
- Security groups for network-level access control

## 🔮 Future Enhancements

- [ ] Implement distributed tracing with AWS X-Ray
- [ ] Add API rate limiting and throttling
- [ ] Implement Circuit Breaker pattern with Resilience4j
- [ ] Add caching layer with Redis
- [ ] Implement health checks and auto-scaling
- [ ] Set up CI/CD pipeline with GitHub Actions
- [ ] Add comprehensive unit and integration tests
- [ ] Implement API documentation with Swagger/OpenAPI
- [ ] Add monitoring dashboards with CloudWatch/Grafana
- [ ] Implement audit logging
- [ ] Add support for file uploads (patient documents)
- [ ] Implement notification service (email/SMS)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Your Name**
- GitHub: https://github.com/NithishKarth1ck
- LinkedIn: https://www.linkedin.com/in/nithish-karthick-993447355 
- Email: nithishnickzz@gmail.com

## 🙏 Acknowledgments

- Spring Boot documentation
- AWS documentation and tutorials
- gRPC Java documentation
- Apache Kafka documentation

---

**⭐ If you found this project helpful, please consider giving it a star!**
```

### `LICENSE` (MIT License)
```
MIT License

Copyright (c) 2026 Your Name

Permission is hereby granted, free of charge, to any person obtaining a copy...
