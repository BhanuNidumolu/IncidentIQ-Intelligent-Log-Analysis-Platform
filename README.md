# IncidentIQ – Intelligent Log Analysis Platform  
### 🚀 AI-Powered Log Ingestion, Semantic Search & Root Cause Analysis

IncidentIQ is an end-to-end AI-driven log analysis platform built using **Spring Boot**, **Redis**, **Vector Search**, **Spring AI**, **Gemini API**, and **Docker**.  
It ingests large-scale logs, generates embeddings, stores them in a vector index, and enables semantic search to detect issues and predict root causes.

---

## 📌 Features

### 🔍 **AI-Driven Semantic Log Search**
- Extracts log chunks and creates vector embeddings.
- Performs high-accuracy semantic search using Redis Vector DB.
- Retrieves contextually relevant logs instead of simple keyword matching.

### ⚙️ **High-Throughput Log Ingestion Pipeline**
- Chunking of logs for optimal embedding generation.
- Async processing for high performance.
- Metadata enrichment (timestamp, severity, file name).

### 🧠 **Root Cause Analysis (RCA)**
- Uses **Gemini 2.5 Pro** via **Spring AI**.
- Performs multi-log reasoning.
- Produces actionable RCA insights.

### 📦 **Tech Stack**
- **Java 17**
- **Spring Boot 3+**
- **Spring AI**
- **Redis Stack** (Vector Search enabled)
- **Gemini Embedding API**
- **Docker**
- **Postman/Insomnia** for testing

---

## 🏛️ Architecture

```text
Log File → Chunker → Embedding Generator (Gemini) → Redis Vector Store → Semantic Search → RCA Engine → Insight Response
```
🛠️ Setup Instructions
1️⃣ Clone the Repository
```
git clone https://github.com/BhanuNidumolu/IncidentIQ-Intelligent-Log-Analysis-Platform.git
cd IncidentIQ-Intelligent-Log-Analysis-Platform
````
2️⃣ Configure Environment Variables

Create a .env or export variables:
```
API_KEY=your_gemini_api_key
```
3️⃣ Start Redis Stack (Vector Search)
```
docker run -d --name redis-stack -p 6379:6379 redis/redis-stack:latest
```
4️⃣ Run Spring Boot App
```
./mvnw spring-boot:run
```
📡 API Endpoints
1. Ingest Logs
POST /logs/ingest

```
Body:

{
  "text": "your log content..."
}

2. Semantic Search
POST /insight


Body:

{
  "query": "service crashed due to timeout"
}
```
📁 Project Structure
```
src/main/java/com/incidentiq/
│
├── controller/        # REST Controllers
├── service/           # Log ingestion, embedding, RCA logic
├── config/            # Redis & AI configurations
└── model/             # Domain models
```
🐳 Docker Support

Build the Image:

docker build -t incidentiq .


Run:

docker run -p 8080:8080 incidentiq

🧪 Testing

Use Postman collection provided in the repo (if added)
or hit endpoints manually.

✨ Future Enhancements

Multi-file ingestion via UI upload

Log anomaly detection using ML

Real-time streaming ingestion (Kafka)

Dashboard with visual RCA insights

🤝 Contributing

Pull Requests are welcome.
Feel free to open issues for feature suggestions.
