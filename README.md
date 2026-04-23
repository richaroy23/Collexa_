# 🎓 Collexa — University Social Network

A full-stack campus social platform built for **CGC Jhanjeri** students.

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | HTML + CSS + Vanilla JS (or swap for React) |
| Backend | Python + Flask |
| Database | MongoDB |
| Auth | JWT (Flask-JWT-Extended) + Bcrypt |
| AI | Google Gemini 1.5 Flash |

---

## 🚀 Quick Start

### 1. Prerequisites

- Python 3.10+
- MongoDB running locally (or MongoDB Atlas URI)
- Node.js (optional, only if running a dev server for frontend)
- Gemini API key (free at https://aistudio.google.com/app/apikey)

---

### 2. Backend Setup

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate        # Linux/Mac
venv\Scripts\activate           # Windows

# Install dependencies
pip install -r requirements.txt

# Configure environment
cp .env.example .env
# Edit .env and add your GEMINI_API_KEY and MONGO_URI
```

### 3. Configure `.env`

```env
MONGO_URI=mongodb://localhost:27017/
JWT_SECRET_KEY=your-random-secret-key-here
GEMINI_API_KEY=AIzaSy...your_key_here
```

### 4. Run the Backend

```bash
python app.py
# Server starts at http://localhost:5000
```

### 5. Seed the Database

```bash
curl -X POST http://localhost:5000/api/seed
```
This adds sample events, opportunities, and study groups.

### 6. Open the Frontend

Simply open `frontend/index.html` in your browser.

Or serve it with Python:
```bash
cd frontend
python -m http.server 8080
# Open http://localhost:8080
```

---

## 🔑 Authentication Rules

- **Only** `@cgcjhanjeri.in` email addresses can register
- Passwords are hashed with bcrypt
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

### AI (Gemini)
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
- ✅ AI Study Buddy powered by Google Gemini
- ✅ JWT-based secure sessions
- ✅ MongoDB persistence

---

## 🔐 Getting a Gemini API Key (Free)

1. Go to https://aistudio.google.com/app/apikey
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the key into your `.env` file as `GEMINI_API_KEY`

Free tier gives 15 requests/minute — perfect for a hackathon demo!

---

## 🏆 Hackathon Presentation Tips

1. **Demo flow**: Register → Post in feed → RSVP event → Ask AI a question
2. **Highlight**: Show the email domain restriction during signup
3. **AI demo**: Ask "Explain Belady's Anomaly" or "Quiz me on DBMS"
4. **Judges love**: Live MongoDB Atlas dashboard showing real-time data

---

## 📂 Project Structure

```
collexa/
├── backend/
│   ├── app.py              # Flask API (auth, posts, events, AI)
│   ├── requirements.txt    # Python dependencies
│   ├── .env.example        # Environment variable template
│   └── .env                # Your actual secrets (don't commit!)
└── frontend/
    └── index.html          # Complete single-file frontend
```

---

Built with ❤️ for CGC Jhanjeri students
