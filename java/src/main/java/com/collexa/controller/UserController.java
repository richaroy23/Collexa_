package com.collexa.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Projections.exclude;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final MongoDatabase db;

    public UserController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> users() { return db.getCollection("users"); }
    private MongoCollection<Document> posts()  { return db.getCollection("posts"); }
    private MongoCollection<Document> resources() { return db.getCollection("resources"); }
    private MongoCollection<Document> groups()    { return db.getCollection("groups"); }

    private Map<String, Object> docToMap(Document doc) {
        Map<String, Object> m = new LinkedHashMap<>(doc);
        if (doc.getObjectId("_id") != null) {
            m.put("_id", doc.getObjectId("_id").toHexString());
        }
        m.remove("password");
        return m;
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "") String q) {
        List<Map<String, Object>> result = new ArrayList<>();
        var filter = q.isEmpty() ? new Document()
                : regex("name", Pattern.compile(q, Pattern.CASE_INSENSITIVE));

        for (Document u : users().find(filter).projection(exclude("password")).limit(20)) {
            result.add(docToMap(u));
        }
        return ResponseEntity.ok(Map.of("users", result));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<?> getUser(@PathVariable String uid) {
        ObjectId oid;
        try { oid = new ObjectId(uid); } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad id"));
        }
        Document u = users().find(eq("_id", oid)).projection(exclude("password")).first();
        if (u == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

        Map<String, Object> res = docToMap(u);
        res.put("post_count",      posts().countDocuments(eq("author_id", uid)));
        res.put("resource_count",  resources().countDocuments(eq("uploader_id", uid)));
        res.put("group_count",     groups().countDocuments(eq("members", uid)));
        List<?> followers = (List<?>) u.getOrDefault("followers", new ArrayList<>());
        List<?> following = (List<?>) u.getOrDefault("following", new ArrayList<>());
        res.put("follower_count",  followers.size());
        res.put("following_count", following.size());
        return ResponseEntity.ok(Map.of("user", res));
    }

    @PostMapping("/{uid}/follow")
    public ResponseEntity<?> followUser(@PathVariable String uid) {
        String me = currentUserId();
        if (me.equals(uid)) return ResponseEntity.badRequest().body(Map.of("error", "Cannot follow yourself"));

        Document target = users().find(eq("_id", new ObjectId(uid))).first();
        if (target == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        List<String> followers = (List<String>) target.getOrDefault("followers", new ArrayList<>());
        if (followers.contains(me)) {
            users().updateOne(eq("_id", new ObjectId(uid)), pull("followers", me));
            users().updateOne(eq("_id", new ObjectId(me)),  pull("following", uid));
            return ResponseEntity.ok(Map.of("following", false, "follower_count", followers.size() - 1));
        }
        users().updateOne(eq("_id", new ObjectId(uid)), push("followers", me));
        users().updateOne(eq("_id", new ObjectId(me)),  push("following", uid));
        return ResponseEntity.ok(Map.of("following", true, "follower_count", followers.size() + 1));
    }

    @GetMapping("/{uid}/posts")
    public ResponseEntity<?> userPosts(@PathVariable String uid) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document p : posts().find(eq("author_id", uid))
                .sort(new Document("created_at", -1)).limit(20)) {
            result.add(docToMap(p));
        }
        return ResponseEntity.ok(Map.of("posts", result));
    }
}
