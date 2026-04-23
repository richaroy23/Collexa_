package com.collexa.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final MongoDatabase db;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.model}")
    private String groqModel;

    public AiController(MongoDatabase db) {
        this.db = db;
    }

    private String uid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String fallbackAi(String msg) {
        String m = msg.toLowerCase();

        if (containsAny(m, "hello", "hi", "hey", "namaste"))
            return "👋 Hello! I'm your Collexa Study Buddy. Ask me anything about your subjects, exam prep, or study tips!";

        if (containsAny(m, "dsa", "data structure", "algorithm"))
            return "📚 **DSA Tips:**\n\n• Master arrays, linked lists, trees, graphs\n• Practice Big-O analysis for every solution\n• LeetCode pattern: Two Pointers, Sliding Window, BFS/DFS, DP\n• For placements: aim for 200+ LeetCode problems\n\nWhat specific topic — sorting, trees, DP, graphs?";

        if (containsAny(m, "dbms", "database", "sql"))
            return "🗄️ **DBMS Key Concepts:**\n\n• **ACID** — Atomicity, Consistency, Isolation, Durability\n• **Normalization** — 1NF → 2NF → 3NF → BCNF\n• **Joins** — INNER, LEFT, RIGHT, FULL OUTER\n• **Indexing** — B+ Trees speed up queries\n• **Transactions** — locks, deadlocks, 2PL\n\nWhat topic do you need help with?";

        if (containsAny(m, "os", "operating system", "process", "thread"))
            return "💻 **OS Important Topics:**\n\n• **Process vs Thread** — process has own memory; threads share\n• **Scheduling** — FCFS, SJF, Round Robin, Priority\n• **Deadlock** — Prevention, Avoidance (Banker's algo), Detection\n• **Memory** — Paging, Segmentation, Virtual Memory, Page Replacement (LRU, FIFO)\n• **Semaphores & Mutex** — for synchronization\n\nAny specific concept?";

        if (containsAny(m, "cn", "computer network", "tcp", "http", "osi"))
            return "🌐 **Computer Networks:**\n\n• **OSI Model** — 7 layers (Physical→Application)\n• **TCP vs UDP** — TCP reliable+slow, UDP fast+unreliable\n• **HTTP vs HTTPS** — HTTPS uses SSL/TLS encryption\n• **DNS** — translates domain names to IPs\n• **Subnetting** — CIDR notation, subnet masks\n\nWhat do you need to go deeper on?";

        if (containsAny(m, "placement", "interview", "job"))
            return "💼 **Placement Prep Roadmap:**\n\n1. **DSA** — Arrays, Strings, Trees, Graphs, DP (LeetCode Medium)\n2. **CS Fundamentals** — DBMS, OS, CN, OOPs\n3. **Projects** — 2-3 strong projects on GitHub\n4. **Aptitude** — Quantitative, Logical, Verbal\n5. **Resume** — 1 page, ATS-friendly\n\n🎯 Target: 3-4 months of consistent prep. Which area to focus first?";

        if (containsAny(m, "study plan", "schedule", "timetable"))
            return "📅 **7-Day Study Plan Template:**\n\n• **Mon/Tue** — Theory (notes + concepts)\n• **Wed/Thu** — Practice problems\n• **Fri** — Revision + weak areas\n• **Sat** — Mock test / PYQs\n• **Sun** — Rest + light review\n\n💡 Tip: Pomodoro technique — 25 min focus, 5 min break. What subject should I plan for?";

        if (containsAny(m, "oops", "oop", "object", "class"))
            return "🔷 **OOPs Concepts:**\n\n• **Encapsulation** — data hiding (private members)\n• **Inheritance** — reusing parent class properties\n• **Polymorphism** — one interface, many forms (overloading/overriding)\n• **Abstraction** — hiding implementation details\n\n**Key for interviews:** Explain with real-world examples!";

        if (containsAny(m, "cgc", "college", "jhanjeri"))
            return "🎓 **CGC Jhanjeri** is one of Punjab's top engineering colleges. Use Collexa to connect with batchmates, join study groups, find internships and collaborate on projects. You've got this! 💪";

        if (containsAny(m, "quiz", "test me", "question"))
            return "📝 **Quick Quiz — DSA:**\n\n**Q1.** What is the time complexity of binary search?\n**Q2.** Difference between Stack and Queue?\n**Q3.** What data structure does BFS use?\n\nReply with your answers and I'll check them! Or tell me which subject to quiz you on.";

        if (m.contains("thank"))
            return "😊 You're welcome! Keep studying hard — CGC Jhanjeri is proud of you! 🎓";

        return "🤔 You asked: *\"" + msg + "\"*\n\nI can help with:\n• **DSA** — algorithms, data structures\n• **DBMS** — SQL, normalization, transactions\n• **OS** — processes, memory, scheduling\n• **CN** — networking, protocols\n• **OOPs** — concepts and design\n• **Placement prep** — roadmap & tips\n• **Study plans** — scheduling & techniques\n\nTo unlock full AI (any topic), add a free Groq key to `.env` → [console.groq.com](https://console.groq.com)";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> aiChat(@RequestBody Map<String, Object> body) {
        String msg = ((String) body.getOrDefault("message", "")).trim();
        List<Map<String, Object>> history = (List<Map<String, Object>>) body.getOrDefault("history", new ArrayList<>());

        if (msg.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Empty message"));

        if (groqApiKey == null || groqApiKey.isEmpty()) {
            return ResponseEntity.ok(Map.of("reply", fallbackAi(msg)));
        }

        // Build messages array
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are Collexa AI Study Buddy for CGC Jhanjeri students. Help with concepts, exam prep, quizzes and study plans. Be concise, clear and encouraging."));

        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            Map<String, Object> h = history.get(i);
            String role    = (String) h.get("role");
            String content = (String) h.get("content");
            if ((role.equals("user") || role.equals("assistant")) && content != null && !content.isEmpty()) {
                messages.add(Map.of("role", role, "content", content));
            }
        }
        messages.add(Map.of("role", "user", "content", msg));

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model",       groqModel);
            requestBody.put("messages",    messages);
            requestBody.put("max_tokens",  1024);
            requestBody.put("temperature", 0.7);

            String json = objectMapper.writeValueAsString(requestBody);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type",  "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<?, ?> resp = objectMapper.readValue(response.body(), Map.class);
                List<?> choices = (List<?>) resp.get("choices");
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                return ResponseEntity.ok(Map.of("reply", message.get("content")));
            } else {
                Map<?, ?> errResp = objectMapper.readValue(response.body(), Map.class);
                Map<?, ?> err = (Map<?, ?>) errResp.getOrDefault("error", Map.of());
                String errMsg = (String) err.getOrDefault("message", "check API key");
                String reply = fallbackAi(msg) + "\n\n_(Groq error: " + errMsg + ")_";
                return ResponseEntity.ok(Map.of("reply", reply));
            }
        } catch (Exception e) {
            String reply = fallbackAi(msg) + "\n\n_(Could not reach Groq: " + e.getMessage() + ")_";
            return ResponseEntity.ok(Map.of("reply", reply));
        }
    }
}
