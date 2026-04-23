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
@RequestMapping("/api/posts")
public class PostController {

    private final MongoDatabase db;

    public PostController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> posts() { return db.getCollection("posts"); }
    private MongoCollection<Document> users() { return db.getCollection("users"); }

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
    public ResponseEntity<?> getPosts(@RequestParam(defaultValue = "") String q) {
        var filter = q.isEmpty() ? new Document()
                : regex("body", Pattern.compile(q, Pattern.CASE_INSENSITIVE));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document p : posts().find(filter).sort(new Document("created_at", -1)).limit(30)) {
            result.add(docToMap(p));
        }
        return ResponseEntity.ok(Map.of("posts", result));
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> body) {
        String uid   = uid();
        Document u   = users().find(eq("_id", new ObjectId(uid))).first();
        String text  = ((String) body.getOrDefault("body",  "")).trim();
        String image = (String) body.getOrDefault("image", "");
        String tag   = (String) body.getOrDefault("tag",   "general");

        if (text.isEmpty() && image.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Need text or image"));

        Document doc = new Document("author_id",   uid)
                .append("author_name", u.getString("name"))
                .append("author_dept", u.getOrDefault("department", ""))
                .append("body",        text)
                .append("image",       image)
                .append("tag",         tag)
                .append("likes",       new ArrayList<>())
                .append("comments",    new ArrayList<>())
                .append("created_at",  Instant.now().toString());

        posts().insertOne(doc);
        return ResponseEntity.status(201).body(Map.of("post", docToMap(doc)));
    }

    @PostMapping("/{pid}/like")
    public ResponseEntity<?> likePost(@PathVariable String pid) {
        String uid = uid();
        Document p = posts().find(eq("_id", new ObjectId(pid))).first();
        if (p == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

        List<String> likes = (List<String>) p.getOrDefault("likes", new ArrayList<>());
        if (likes.contains(uid)) {
            posts().updateOne(eq("_id", new ObjectId(pid)), pull("likes", uid));
            return ResponseEntity.ok(Map.of("liked", false, "count", likes.size() - 1));
        }
        posts().updateOne(eq("_id", new ObjectId(pid)), push("likes", uid));
        return ResponseEntity.ok(Map.of("liked", true, "count", likes.size() + 1));
    }

    @PostMapping("/{pid}/comment")
    public ResponseEntity<?> comment(@PathVariable String pid, @RequestBody Map<String, String> body) {
        String uid  = uid();
        Document u  = users().find(eq("_id", new ObjectId(uid))).first();
        String text = body.getOrDefault("text", "").trim();
        if (text.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Empty comment"));

        Document c = new Document("author_id",   uid)
                .append("author_name", u.getString("name"))
                .append("author_dept", u.getOrDefault("department", ""))
                .append("text",        text)
                .append("created_at",  Instant.now().toString());

        posts().updateOne(eq("_id", new ObjectId(pid)), push("comments", c));
        return ResponseEntity.status(201).body(Map.of("comment", new LinkedHashMap<>(c)));
    }
}
