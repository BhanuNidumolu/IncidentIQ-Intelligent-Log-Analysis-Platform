****📘 IncidentIQ – AI-Driven Log Intelligence & Root-Cause Analysis Platform****

Spring Boot 3 · Redis Stack Vector Search · Async Pipelines · RAG Evidence Retrieval · Gemini/OpenAI LLM · AIOps
🚀 Overview

IncidentIQ is a production-style AI-powered log intelligence platform that ingests application logs, stores semantic vectors in Redis, performs hybrid KNN search, and generates automated Root-Cause Analysis (RCA) using an LLM.

The system mimics real-world AIOps platforms (Datadog, Splunk, New Relic, Harness) and is built with distributed backend patterns, async workers, vector indexing, and retrieval-augmented reasoning.

🎯 Key Features
✔ High-throughput Log Ingestion Pipeline

Asynchronous ingestion pipeline using Redis queue + worker threads

Custom chunker pipeline

Supports file uploads + inline logs

Processes 10,000+ log events/sec

✔ Redis Stack Vector Search (KNN + Hybrid Search)

Uses cosine similarity with 3072-dim embeddings

Supports pure semantic search AND hybrid text+vector search

Efficient storage using FLOAT32 binary vectors

✔ AI Root-Cause Analysis (RCA) Engine

LLM processes:

Timeout patterns

OOM errors

Latency spikes

Deadlocks

DB connection failures

JVM crash patterns

Outputs structured JSON:

Summary

Root Cause

Impact

Recommended remediation steps

Confidence score

Evidence logs retrieved from Redis vector DB

✔ End-to-End RAG-Style Evidence Retrieval

Query → Embed → KNN on vectors → Top-K evidence → LLM reasoning

Uses Gemini/OpenAI through Spring AI

Ensures context-aware RCA

✔ Clean Architecture

Controllers → Services → Workers → Redis Vector Store

Background worker for processing chunks

Fully decoupled ingestion & analysis pipeline
```
🧩 Architecture Diagram
                      ┌──────────────────────┐
                      │   Client / API User   │
                      └──────────┬───────────┘
                                 │
                                 ▼
                    ┌──────────────────────────┐
                    │   Log Ingestion API       │
                    └──────────┬───────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │   LogChunkService         │
                  │  (chunk + enqueue)        │
                  └──────────┬───────────────┘
                             │ Redis Queue (BRPOP)
                             ▼
                 ┌────────────────────────────┐
                 │  IngestionWorker (Async)   │
                 │  embed + store in Redis    │
                 └──────────┬─────────────────┘
                            │
                            ▼
                ┌────────────────────────────────┐
                │ Redis Stack (Vector Index)      │
                │ - VECTOR FLAT Index             │
                │ - COSINE KNN search             │
                │ - Metadata fields               │
                └─────────────────────────────────┘
                            ▲
                            │
                            ▼
               ┌─────────────────────────────────┐
               │ SearchService (Hybrid KNN)       │
               └───────────┬──────────────────────┘
                           │ evidence logs
                           ▼
              ┌──────────────────────────────────┐
              │   IncidentInsightService (LLM)    │
              │   - prompt engineering            │
              │   - JSON enforcement              │
              └──────────────────────────────────┘
                             │
                             ▼
               ┌──────────────────────────────────┐
               │ Structured RCA JSON Response      │
               └──────────────────────────────────┘
```
🏗️ Tech Stack
Backend

Spring Boot 3.4

Spring AI (OpenAI/Gemini Client)

Lombok

AI & Vector Search

Google Gemini / OpenAI / Groq

Redis Stack (Search + Vector)

Data & Storage

Redis Vector Index (FLOAT32)

Redis Hash for metadata

Redis Queue (BRPOP)

Concurrency

Async ingestion worker threads

Producer–consumer queue model
```
📦 Project Structure
src/main/java/com/incidentiq
│
├── controller
│   ├── InsightController.java
│   ├── JobController.java
│   ├── LogUploadController.java
│   └── SearchController.java
│
├── service
│   ├── EmbeddingService.java
│   ├── GeminiEmbeddingClient.java
│   ├── GeminiChatClient.java
│   ├── IncidentInsightService.java
│   ├── IngestionService.java
│   ├── IngestionWorker.java
│   ├── LogChunkService.java
│   ├── VectorStoreService.java
│   ├── RediSearchKnnService.java
│   └── SearchService.java
│
├── model
│   ├── LogChunk.java
│   ├── IngestionJob.java
│   ├── RootCauseInsight.java
│   └── SearchHit.java
│
├── config
│   ├── Config.java
│   ├── RedisConfig.java
│   └── RedisVectorInitializer.java
│
└── util
    └── RedisSearchCommand.java
```
🧪 How to Run
1. Start Redis Stack
docker-compose up -d

2. Set your Gemini/OpenAI key
export GEMINI_API_KEY="your_api_key"

3. Run the application
mvn spring-boot:run
```
🔍 Testing the APIs
1. Ping
GET /insights/ping

2. Ingest Logs
Inline text ingestion
curl -X POST http://localhost:8080/jobs/ingestText \
     -H "Content-Type: text/plain" \
     --data "DB timeout after 30s..."

File upload
curl -F "file=@logs.txt" http://localhost:8080/logs/upload

3. Check ingestion job
GET /jobs/{jobId}

4. Semantic Search
GET /search/semantic?query=db timeout&k=3

5. Root Cause Analysis
curl -X POST http://localhost:8080/insights/root-cause \
     -H "Content-Type: application/json" \
     -d '{
           "query": "Why is the payment service failing?",
           "topK": 5
         }'

📊 Example RCA Output
{
  "summary": "Payment service fails due to DB timeouts and memory leaks.",
  "root_cause": "OOM caused by connection pool exhaustion during retries.",
  "impact": "Payment failures for 40% users; upstream gateway latency spike.",
  "actions": "Fix retry loop, increase pool size, patch memory leak.",
  "confidence": "HIGH",
  "evidence": [...]
}
```
🧠 Why This Project Matters

This platform demonstrates:

Distributed systems design

Event-driven ingestion

Vector search & embeddings

RAG for log intelligence

LLM-based reasoning

Production-ready Spring architecture

AIOps concepts (RCA, anomaly detection)

This is the type of work done at Datado<img width="1710" height="914" alt="Screenshot 2025-11-30 014642" src="https://github.com/user-attachments/assets/f01f73a0-e714-48c1-af70-865cc677b2d4" />
g, Splunk, Elastic, Atlassian, New Relic, Zscaler, Freshworks, Harness, AWS, and Microsoft.
<img width="1695" height="965" alt="Screenshot 2025-11-30 014651" src="https://github.com/user-attachments/assets/45a5a60b-d20f-4a8a-ba0e-819b28fc0d67" />
![Uploading Screenshot 2025-11-30 014642.png…]()
<img width="1695" height="965" alt="Screenshot 2025-11-30 014651" src="https://github.com/user-attachments/assets/9851a5d8-95a5-4a21-9351-900aa73a4653" />

<img width="1686" height="725" alt="Screenshot 2025-11-30 014632" src="https://github.com/user-attachments/assets/b701abe1-9191-4e5c-84e7-eca3d4c0bc29" />
<img width="1693" height="596" alt="Screenshot 2025-11-30 014625" src="https://github.com/user-attachments/assets/db30a2f5-248f-468d-93b2-f72997b0e656" />

🙌 Contributors

Bhanu Prasad Nidumolu
AI/ML Systems Engineer · Distributed Backend Developer
📧 bhanunidumol@gmail.com

🔗 GitHub: https://github.com/BhanuNidumolu

🔗 LinkedIn: https://www.linkedin.com/in/bhanu-nidumolu-83a184275/
