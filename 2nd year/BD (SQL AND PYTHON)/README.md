# ✈️ Aviation Database Management System - Database

## 📋 Project Overview
This project involves the design, implementation, and optimization of a robust database system for an airline company ("Aviacao") 🛫. Developed using **PostgreSQL**, the system manages complex relationships between flights, aircraft, and ticket sales, ensuring high data integrity and analytical capability through a custom REST API.

## ✨ Key Features
- 🐘 **PostgreSQL Powered**: Advanced database implementation with complex relational schemas.
- 🛠️ **Integrity Triggers**: Custom triggers for real-time validation of seat assignments, capacity, and sale timings.
- 🌐 **RESTful API**: Python-based web service providing JSON endpoints for programmatic booking and check-in.
- 📊 **OLAP Analytics**: Advanced data analysis using materialized views and multidimensional SQL queries.
- ⚡ **Performance Optimized**: Strategic indexing with theoretical and practical justification via `EXPLAIN ANALYSE`.
- 🏗️ **ACID Compliant**: Guaranteed atomicity in transactions to prevent overbooking and data corruption.
- 🏆 Achieved a grade of **17.00/20** (Group Project)

## 📌 Core Functionalities
The application handles:
1. 🎟️ **Ticketing & Check-in**: Automatic seat assignment ensuring class and aircraft consistency.
2. 🕒 **Operational Logic**: Strict constraints ensuring sales occur before departures and flights follow logical routes.
3. 🗺️ **Network Management**: Coordination of daily flights across 10+ international European airports.
4. 📈 **Business Intelligence**: Reporting on route popularity, fleet usage, and global profitability.

## 📂 Project Structure
The repository contains:
- 📓 **Jupyter Notebook**: Full technical report containing SQL implementations and analytical results.
- 💻 **App Folder**: Source code for the RESTful API developed for the project.
- 🗃️ **Data Folder**: Population scripts (`populate.sql`) containing over 30,000 realistic records.
- 🧪 **SQL Queries**: Comprehensive scripts for views, triggers, and OLAP operations.

## 🎯 Grade & Recognition
This project received a high score of **17.00 out of 20** 📈