package com.collexa.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final MongoDatabase db;

    public SearchController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> users()     { return db.getCollection("users"); }
    private MongoCollection<Document> posts()     { return db.getCollection("posts"); }
    private MongoCollection<Document> events()    { return db.getCollection("events"); }
    private MongoCollection<Document> resources() { return db.getCollection("resources"); }
    private MongoCollection<Document> groups()    { return db.getCollection("groups"); }
    private MongoCollection<Document> market()    { return db.getCollection("marketplace"); }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(defaultValue = "") String q) {
        if (q.length() < 2) return ResponseEntity.ok(Map.of("results", new ArrayList<>()));

        Pattern rx = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Document u : users().find(regex("name", rx)).limit(5)) {
            results.add(Map.of(
                    "type", "user", "id", u.getObjectId("_id").toHexString(),
                    "title", u.getString("name"),
                    "sub", u.getOrDefault("department", "") + " · " + u.getOrDefault("year", ""),
                    "emoji", "👤"
            ));
        }
        for (Document p : posts().find(regex("body", rx)).limit(4)) {
            String body = p.getString("body");
            if (body == null) body = "";
            results.add(Map.of(
                    "type", "post", "id", p.getObjectId("_id").toHexString(),
                    "title", body.length() > 55 ? body.substring(0, 55) + "…" : body,
                    "sub",   "by " + p.getOrDefault("author_name", ""),
                    "emoji", "📝"
            ));
        }
        for (Document e : events().find(regex("title", rx)).limit(4)) {
            results.add(Map.of(
                    "type", "event", "id", e.getObjectId("_id").toHexString(),
                    "title", e.getOrDefault("title", ""),
                    "sub",   e.getOrDefault("date", "") + " · " + e.getOrDefault("location", ""),
                    "emoji", e.getOrDefault("emoji", "🎯")
            ));
        }
        for (Document r : resources().find(or(regex("title", rx), regex("subject", rx))).limit(4)) {
            results.add(Map.of(
                    "type", "resource", "id", r.getObjectId("_id").toHexString(),
                    "title", r.getOrDefault("title", ""),
                    "sub",   r.getOrDefault("subject", ""),
                    "emoji", "📚"
            ));
        }
        for (Document g : groups().find(regex("name", rx)).limit(4)) {
            List<?> members = (List<?>) g.getOrDefault("members", new ArrayList<>());
            results.add(Map.of(
                    "type", "group", "id", g.getObjectId("_id").toHexString(),
                    "title", g.getOrDefault("name", ""),
                    "sub",   members.size() + " members",
                    "emoji", g.getOrDefault("emoji", "👥")
            ));
        }
        for (Document m : market().find(regex("title", rx)).limit(4)) {
            results.add(Map.of(
                    "type", "marketplace", "id", m.getObjectId("_id").toHexString(),
                    "title", m.getOrDefault("title", ""),
                    "sub",   "₹" + m.getOrDefault("price", "") + " · " + m.getOrDefault("poster_name", ""),
                    "emoji", "🛒"
            ));
        }

        return ResponseEntity.ok(Map.of("results", results));
    }
}
