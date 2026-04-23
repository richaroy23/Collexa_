package com.collexa.controller;

import com.collexa.security.JwtUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MongoDatabase db;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.domain}")
    private String domain;

    public AuthController(MongoDatabase db, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.db = db;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    private MongoCollection<Document> users() {
        return db.getCollection("users");
    }

    private Map<String, Object> safeUser(Document u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getObjectId("_id").toHexString());
        m.put("name", u.getString("name"));
        m.put("email", u.getString("email"));
        m.put("department", u.getOrDefault("department", ""));
        m.put("year", u.getOrDefault("year", ""));
        m.put("bio", u.getOrDefault("bio", ""));
        m.put("followers", u.getOrDefault("followers", new ArrayList<>()));
        m.put("following", u.getOrDefault("following", new ArrayList<>()));
        return m;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String name  = body.getOrDefault("name",  "").trim();
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String pw    = body.getOrDefault("password", "");
        String dept  = body.getOrDefault("department", "").trim();
        String yr    = body.getOrDefault("year", "").trim();

        if (name.isEmpty() || email.isEmpty() || pw.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password required"));
        if (!email.endsWith(domain))
            return ResponseEntity.badRequest().body(Map.of("error", "Only " + domain + " emails allowed"));
        if (pw.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password min 6 chars"));

        if (users().find(eq("email", email)).first() != null)
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));

        String hashed = passwordEncoder.encode(pw);
        Document doc = new Document("name", name)
                .append("email", email)
                .append("password", hashed)
                .append("department", dept)
                .append("year", yr)
                .append("bio", "")
                .append("followers", new ArrayList<>())
                .append("following", new ArrayList<>())
                .append("joined", Instant.now().toString());

        users().insertOne(doc);
        String token = jwtUtil.generateToken(doc.getObjectId("_id").toHexString());
        return ResponseEntity.status(201).body(Map.of("token", token, "user", safeUser(doc)));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String pw    = body.getOrDefault("password", "");

        if (!email.endsWith(domain))
            return ResponseEntity.badRequest().body(Map.of("error", "Only " + domain + " emails"));

        Document u = users().find(eq("email", email)).first();
        if (u == null || !passwordEncoder.matches(pw, u.getString("password")))
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));

        String token = jwtUtil.generateToken(u.getObjectId("_id").toHexString());
        return ResponseEntity.ok(Map.of("token", token, "user", safeUser(u)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        String uid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Document u = users().find(eq("_id", new ObjectId(uid))).first();
        if (u == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        return ResponseEntity.ok(Map.of("user", safeUser(u)));
    }
}
