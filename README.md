# 🎓 Collexa — University Social Network

A full-stack campus social platform built for **CGC Jhanjeri** students.

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | HTML + CSS + Vanilla JS (single-file) |
| Backend | Java 21+ + Spring Boot 3.2.0 |
| Database | MongoDB Atlas (cloud) |
| Auth | JWT (JJWT 0.12.3) + BCrypt (Spring Security) |
| AI | GROQ API (Llama 3.3 70B) |

---

## 🚀 Quick Start

### 1. Prerequisites

- Java 21 or higher ([Download](https://adoptium.net/))
- Maven 3.9+ ([Download](https://maven.apache.org/download.cgi))
- A MongoDB Atlas account and cluster ([Free at cloud.mongodb.com](https://cloud.mongodb.com))
- A GROQ API key (free at [console.groq.com/keys](https://console.groq.com/keys))

---

### 2. Install Maven (if not installed)

```powershell
# Download Maven
Invoke-WebRequest -Uri "https://dlcdn.apache.org/maven/maven-3/3.9.15/binaries/apache-maven-3.9.15-bin.zip" -OutFile "$env:USERPROFILE\Downloads\maven.zip" -UseBasicParsing

# Extract
Expand-Archive -Path "$env:USERPROFILE\Downloads\maven.zip" -DestinationPath "$env:USERPROFILE\maven" -Force

# Add to PATH (permanent)
[Environment]::SetEnvironmentVariable("Path", [Environment]::GetEnvironmentVariable("Path","User") + ";$env:USERPROFILE\maven\apache-maven-3.9.15\bin", "User")
```

Then **close and reopen PowerShell** — `mvn -version` should work.

---

### 3. Configure MongoDB Atlas

1. Create a free cluster at [cloud.mongodb.com](https://cloud.mongodb.com)
2. Go to **Network Access** → **Add IP Address** → Allow Access from Anywhere
3. Go to your cluster → **Connect** → **Drivers** → Copy the connection string

---

### 4. Configure `.env`

```env
MONGO_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/collexa?retryWrites=true&w=majority&appName=Collexa
JWT_SECRET_KEY=your-long-random-secret-key
GROQ_API_KEY=your-groq-api-key-here
```

> ⚠️ If your password contains special characters like `@`, URL-encode them:
> `@` → `%40` | `#` → `%23` | `%` → `%25`

---

### 5. Run the Backend

```powershell
cd java
mvn spring-boot:run
# API starts at http://localhost:8080
```

---

### 6. Open the Frontend

Simply open `index.html` in your browser — no server needed.

---

## 🔑 Authentication Rules

- **Only** `@cgcjhanjeri.in` email addresses can register
- Passwords are hashed with BCrypt
- JWT tokens expire after 7 days
- Tokens stored in localStorage

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Register new student |
| POST | `/api/auth/signin` | Login |
| GET | `/api/auth/me` | Get current user |

### Feed
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/posts` | Get all posts |
| POST | `/api/posts` | Create a post |
| POST | `/api/posts/:id/like` | Like/unlike a post |

### Events
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | Get all events |
| POST | `/api/events/:id/rsvp` | RSVP/un-RSVP an event |

### Opportunities
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/opportunities` | Get all (filter with `?category=internship`) |

### Study Groups
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/groups` | Get all groups |
| POST | `/api/groups/:id/join` | Join/leave a group |

### AI (GROQ)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/chat` | Send message to AI |
| GET | `/api/ai/history` | Get chat history |

### Utility
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/seed` | Seed sample data |

---

## 🌟 Features

- ✅ University email authentication (`@cgcjhanjeri.in` only)
- ✅ Campus Feed with posts, likes, and tags
- ✅ Events with RSVP system
- ✅ Opportunities Hub (internships, scholarships, competitions, research)
- ✅ Study Groups with join/leave
- ✅ AI Study Buddy powered by GROQ (Llama 3.3 70B)
- ✅ JWT-based secure sessions
- ✅ MongoDB Atlas cloud persistence

---

## 🔐 Getting a GROQ API Key (Free)

1. Go to [console.groq.com/keys](https://console.groq.com/keys)
2. Sign in with your Google account
3. Click **Create API Key**
4. Copy the key into your `.env` file as `GROQ_API_KEY`

Free tier gives fast inference — perfect for a hackathon demo!

---

## 📂 Project Structure

```
collexa/
├── java/                        # Spring Boot backend
│   ├── src/main/java/com/collexa/
│   │   ├── config/              # MongoDB + app config
│   │   ├── controller/          # REST API controllers
│   │   └── security/            # JWT auth + Spring Security
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml                  # Maven dependencies
├── index.html                   # Complete single-file frontend
├── .env                         # Your secrets (don't commit!)
├── .env.example                 # Template for .env
└── README.md
```

---

Built with ❤️ for CGC University students
