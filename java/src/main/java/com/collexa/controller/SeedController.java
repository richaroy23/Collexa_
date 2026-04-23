package com.collexa.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SeedController {

    private final MongoDatabase db;

    public SeedController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> events()    { return db.getCollection("events"); }
    private MongoCollection<Document> opps()      { return db.getCollection("opportunities"); }
    private MongoCollection<Document> groups()    { return db.getCollection("groups"); }

    @PostMapping("/seed")
    public ResponseEntity<?> seed() {
        String now = Instant.now().toString();

        if (events().countDocuments() == 0) {
            events().insertMany(List.of(
                new Document("title","HackMIT 2025").append("desc","48-hour hackathon. $10,000 in prizes!")
                    .append("date","2025-03-12").append("location","Building 32").append("emoji","🏆").append("attendees", new ArrayList<>()),
                new Document("title","AI Research Talk").append("desc","Stanford prof on LLMs and alignment.")
                    .append("date","2025-03-15").append("location","CSAIL Auditorium").append("emoji","🤖").append("attendees", new ArrayList<>()),
                new Document("title","Summer Internship Fair").append("desc","50+ companies recruiting for summer 2025.")
                    .append("date","2025-03-18").append("location","Kresge Oval").append("emoji","💼").append("attendees", new ArrayList<>()),
                new Document("title","UI/UX Bootcamp").append("desc","2-day Figma & design thinking workshop.")
                    .append("date","2025-03-22").append("location","Design Lab").append("emoji","🎨").append("attendees", new ArrayList<>())
            ));
        }

        if (opps().countDocuments() == 0) {
            opps().insertMany(List.of(
                new Document("title","Software Engineering Intern").append("company","Google").append("location","Hybrid")
                    .append("tags", List.of("Python","ML")).append("deadline","2025-04-02").append("category","internship").append("created_at", now),
                new Document("title","Google Summer of Code 2025").append("company","Google Open Source").append("location","Remote")
                    .append("tags", List.of("Open Source","Stipend $3500")).append("deadline","2025-04-02").append("category","competition").append("created_at", now),
                new Document("title","Mitacs Globalink Research").append("company","Mitacs Canada").append("location","Canada")
                    .append("tags", List.of("Research","Fully Funded")).append("deadline","2025-03-28").append("category","research").append("created_at", now),
                new Document("title","Tata Scholarship 2025").append("company","Tata Trusts").append("location","India")
                    .append("tags", List.of("₹3 Lakh/year")).append("deadline","2025-04-15").append("category","scholarship").append("created_at", now)
            ));
        }

        if (groups().countDocuments() == 0) {
            groups().insertMany(List.of(
                new Document("name","Data Structures & Algorithms").append("desc","Daily problems, weekly contests, interview prep.").append("emoji","🌲").append("members", new ArrayList<>()),
                new Document("name","AI/ML Research Club").append("desc","Paper reading, project collab, guest speakers.").append("emoji","🤖").append("members", new ArrayList<>()),
                new Document("name","Competitive Programming").append("desc","Codeforces, LeetCode, ICPC prep.").append("emoji","⚡").append("members", new ArrayList<>()),
                new Document("name","Cloud & DevOps").append("desc","AWS, GCP, Docker, Kubernetes workshops.").append("emoji","☁️").append("members", new ArrayList<>()),
                new Document("name","Cybersecurity Society").append("desc","CTF competitions, ethical hacking workshops.").append("emoji","🔐").append("members", new ArrayList<>()),
                new Document("name","Product & Design").append("desc","UX research, Figma skills, portfolio reviews.").append("emoji","🎨").append("members", new ArrayList<>())
            ));
        }

        return ResponseEntity.ok(Map.of("message", "Seeded!"));
    }

    @GetMapping("/opportunities")
    public ResponseEntity<?> getOpps(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String q) {
        Document filter = new Document();
        if (!category.isEmpty()) filter.append("category", category);
        if (!q.isEmpty()) filter.append("title", new Document("$regex", q).append("$options", "i"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document o : db.getCollection("opportunities").find(filter).sort(new Document("created_at", -1))) {
            Map<String, Object> m = new java.util.LinkedHashMap<>(o);
            if (o.getObjectId("_id") != null) m.put("_id", o.getObjectId("_id").toHexString());
            result.add(m);
        }
        return ResponseEntity.ok(Map.of("opportunities", result));
    }
}
