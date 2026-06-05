# Worker Attendance & Overtime Settlement Engine

----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## Overview

This project extends an existing HRMS system to support **attendance tracking and overtime settlement** for construction workers.

The system is built keeping in mind real-world usage by:

- 👷 Workers (daily wage)
- 👨‍💼 Site Supervisors (real-time tracking)
- 💰 Payroll Operators (monthly settlement)

-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## Tech Stack

- Java 17
- Spring Boot
- Hibernate / JPA
- PostgreSQL (Supabase)
- Redis (Active worker cache)
- Maven

-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## ✅ Setup Instructions

### 1. Clone Repository
```bash
git clone <repo_link>
cd project
```
### 2. Setup Supabase (PostgreSQL)

1.Create account: https://supabase.com

2.Create a new project

3.Copy connection string
```bash
spring.datasource.url=jdbc:postgresql://localhost:5432/hrms
spring.datasource.username=postgres
spring.datasource.password=postgres
```
✅ Use port 5432 

### 3. Run Redis
```bash
redis-server
```
### 4. Start Application
```bash
mvn spring-boot:run
```
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
### HRMS Repo Used

Spring Boot Employee Management System

Reason:

1.Clean layered architecture

2.Easy to extend payroll and attendance features

-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
### System Architecture

🔹 Request Flow
Frontend → Controller → Service → Repository → Database


🔹 Redis Integration
Clock-in → Redis ADD
Clock-out → Redis REMOVE
GET /active → Redis ONLY


🔹 Settlement Flow



POST /settle


   ↓

   
@Transactional START


   ↓

   
Update all entries


   ↓

   
COMMIT


   ↓

   
After Commit Event


   ↓

   
SMS Notification

--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
### 🔹 High-Level Architecture

1.Users-

👷 Worker   👨‍💼 Supervisor   💰 Payroll

2.REST APIs

3.Service Layer

4.PostgreSQL ->Redis

5.Settlement Event → SMS

-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
### APIs
1.Attendance APIs


i)Clock-in
POST /api/attendance/clock-in

JSON{  "workerId": 1,  "siteId": 2}


ii)Clock-out
POST /api/attendance/clock-out

JSON{  "workerId": 1}


iii)GET /api/attendance/active


iv)GET /api/attendance/log?workerId=1&from=2026-05-01&to=2026-05-31&page=0&size=20



2.Overtime APIs

i)GET /api/overtime/summary/{workerId}?month=YYYY-MM


ii)POST /api/overtime/settle/{workerId}?month=YYYY-MM

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
### Redis Strategy

Key: active_workers

TTL: 16 hours

Removed on clock-out

Used only for /active endpoint

-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

### Fault Tolerance

Redis down → fallback to DB

Application continues functioning

-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

### Business Rules


✅ Clock-in

Worker must exist and be active
No duplicate clock-in
Site must be active


✅ Clock-out

Must have active clock-in
If shift > 16 hours → flagged


✅ Overtime Calculation

Overtime rate: 1.5x daily wage rate for the first 2 overtime hours, 2x beyond that


✅ Monthly Cap

Max overtime = 60 hours

Extra hours are ignored


✅ Settlement

Cannot settle current month

Atomic transaction (all-or-nothing)

Returns total payout
