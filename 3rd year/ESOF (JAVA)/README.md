# HumanaEthica – Shift Management System

This repository contains the implementation of the **Shift Management** module for the HumanaEthica platform. The project was developed as a group assignment for the **Software Engineering** course, focusing on a robust backend refactor and a comprehensive frontend adaptation using Test-Driven Development (TDD).

---

## 🏆 Project Achievement
*   **Final Grade:** 19 / 20
*   **Project Type:** Group Project

---

## 👥 Project Overview
The goal of this project was to simulate a real-world software evolution scenario. We introduced the concept of **Shifts** (Turnos) to Activities, which required a significant structural overhaul of the existing domain model.

### Key Architectural Changes
*   **Granularity:** Activities are now divided into specific time slots (Shifts), each with its own participant limit.
*   **Decoupling:** `Participation` is no longer directly linked to a `Volunteer`. Instead, it is managed through an `Enrollment`.
*   **Validation:** Implementation of strict domain invariants to ensure data consistency across Activities, Shifts, and Enrollments.

---

## 🛠 Features & Implementation

### Part 1: Backend Development (Java / Spring Boot)
The first phase focused on the backend, ensuring business logic and domain constraints were strictly enforced through unit, integration, and component tests.

**Core Invariants Implemented:**
*   **Shift Timing:** Start dates must be before end dates and must fit within the parent Activity’s timeframe.
*   **Capacity Control:** The sum of all Shift capacities cannot exceed the total capacity of the Activity.
*   **Enrollment Rules:** Volunteers can select multiple shifts for an activity, provided the schedules do not overlap.
*   **Security:** Access control based on roles (Institution Members vs. Volunteers/Public).

### Part 2: Frontend & E2E Testing (Vue.js / Cypress)
The second phase involved rebuilding the UI to accommodate the new backend structure and ensuring a seamless user experience.

**Key UI/UX Tasks:**
*   **Management Dashboard:** Interfaces for institution members to create and monitor activity shifts.
*   **Multi-Select Enrollment:** A complex selection tool for volunteers to apply for multiple shifts simultaneously.
*   **Validation Feedback:** Real-time UI validation for string lengths (20-200 characters) and capacity limits.
*   **End-to-End Testing:** Extensive Cypress suites to verify user flows, including edge cases like overlapping schedules and activity status restrictions.

---

## 🚀 Technical Stack
*   **Backend:** Java 17, Spring Boot, JPA/Hibernate.
*   **Frontend:** Vue.js, TypeScript, Vuetify.
*   **Testing:** JUnit 5, Spock Framework (Groovy), Cypress (E2E).
*   **Database:** PostgreSQL.
*   **Tools:** Git (GitLab), Maven, Node.js.

---

## 📈 Evaluation Criteria
The project was evaluated based on four main pillars for every task:
1.  **Code & Revision:** Clean commits and proper peer review.
2.  **Functionality:** Meeting 100% of the specified requirements (Basic and Advanced).
3.  **Test Coverage:** Maximum coverage for new code and regression testing for existing features.
4.  **Code Quality:** Adherence to the system architecture, use of Java Streams, and the "Minimum Code" principle.

---

> **Note:** This project followed a strict "Backend First" delivery policy. The frontend was intentionally disabled during the first phase to prioritize structural integrity before adapting the user interface.