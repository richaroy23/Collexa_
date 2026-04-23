package com.collexa.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MongoDatabase db;

    public MarketplaceController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> market() { return db.getCollection("marketplace"); }
    private MongoCollection<Document> users()  { return db.getCollection("users"); }

    private String uid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Map<String, Object> docToMap(Document doc) {
        Map<String, Object> m = new LinkedHashMap<>(doc);
        if (doc.getObjectId("_id") != null)
            m.put("_id", doc.getObjectId("_id").toHexString());
        return m;
    }

    @GetMapping
    public ResponseEntity<?> getMarket(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String q) {
        Document filter = new Document();
        if (!category.isEmpty()) filter.append("category", category);
        if (!q.isEmpty()) {
            Pattern p = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
            filter.append("$or", List.of(
                    new Document("title",       new Document("$regex", p)),
                    new Document("description", new Document("$regex", p))
            ));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document item : market().find(filter).sort(new Document("created_at", -1)))
            result.add(docToMap(item));
        return ResponseEntity.ok(Map.of("items", result));
    }

    @PostMapping
    public ResponseEntity<?> createMarket(@RequestBody Map<String, Object> body) {
        String uid  = uid();
        Document u  = users().find(eq("_id", new ObjectId(uid))).first();
        String title = ((String) body.getOrDefault("title",       "")).trim();
        String desc  = ((String) body.getOrDefault("description", "")).trim();
        if (title.isEmpty() || desc.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Title and description required"));

        Document doc = new Document("title",       title)
                .append("description",  desc)
                .append("price",        body.getOrDefault("price",   ""))
                .append("category",     body.getOrDefault("category","other"))
                .append("skills",       body.getOrDefault("skills",  new ArrayList<>()))
                .append("contact",      ((String) body.getOrDefault("contact", "")).trim())
                .append("poster_id",    uid)
                .append("poster_name",  u.getString("name"))
                .append("poster_dept",  u.getOrDefault("department", ""))
                .append("status",       "open")
                .append("created_at",   Instant.now().toString());

        market().insertOne(doc);
        return ResponseEntity.status(201).body(Map.of("item", docToMap(doc)));
    }

    @PostMapping("/{mid}/close")
    public ResponseEntity<?> closeMarket(@PathVariable String mid) {
        String uid = uid();
        Document item = market().find(eq("_id", new ObjectId(mid))).first();
        if (item == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        if (!uid.equals(item.getString("poster_id")))
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));

        market().updateOne(eq("_id", new ObjectId(mid)), set("status", "closed"));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
