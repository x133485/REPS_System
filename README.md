# Renewable Energy Plant Monitoring System

This project is a Scala-based console application designed to **monitor, analyze, and manage data** from renewable energy sources, including **solar**, **wind**, and **hydropower**. It uses Fingrid’s open API to fetch real-time energy data and provides functionality to store and process that data for further insights.

---

## ✅ Completed Features

### ✅ API Integration
- Successfully connected to Fingrid API for:
  - **Solar power forecasts** (dataset 248)
  - **Wind generation data** (dataset 181)
  - **Hydropower generation data** (dataset 191, same format as wind)
- API response parsing using uPickle
- Output preview in formatted console printout

### ✅ Data Storage
- Stores fetched solar/wind/hydro data to separate CSV files:
  - `solar.csv`
  - `wind.csv`
  - `hydro.csv`
- Optionally allows **editing data** before saving

### ✅ User Interaction
- Menu-driven console interface with repeatable options
- Menu loop allows continuous operation until the user chooses to exit
- Dynamic display of retrieved data with timestamp ranges and power output

---

## ❌ Not Yet Implemented

### ❌ Data View & Visualization
- Display stored CSV data as a complete dashboard/view
- Allow in-console viewing of data without manually opening the file

### ❌ Data Analysis
- Filtering by:
  - Hourly / Daily / Weekly / Monthly
- Sorting and searching through stored values
- Descriptive statistics:
  - **Mean**: Average of all values
  - **Median**: Middle value of ordered set
  - **Mode**: Most frequently occurring value
  - **Range**: Difference between max and min
  - **Midrange**: Average of max and min values

### ❌ Monitoring & Alerts
- Detect low energy output or anomalies
- Simulate equipment failure scenarios
- Notify operators via alert messages in console

---

## 🛠 Tech Stack

- **Scala 3.3.5**
- **sbt** (build tool)
- **sttp-client3** for HTTP requests
- **uPickle** for JSON parsing
- **Java NIO** for file writing

---

## 🚀 Getting Started

```bash
sbt compile
sbt run
