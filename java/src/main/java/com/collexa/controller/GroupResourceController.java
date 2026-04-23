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
@RequestMapping("/api")
public class GroupResourceController {

    private final MongoDatabase db;

    public GroupResourceController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> groups()    { return db.getCollection("groups"); }
    private MongoCollection<Document> resources() { return db.getCollection("resources"); }
    private MongoCollection<Document> users()     { return db.getCollection("users"); }
    private MongoCollection<Document> groupChats(){ return db.getCollection("group_chats"); }

    private String uid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Map<String, Object> docToMap(Document doc) {
        Map<String, Object> m = new LinkedHashMap<>(doc);
        if (doc.getObjectId("_id") != null)
            m.put("_id", doc.getObjectId("_id").toHexString());
        return m;
    }

    // ── GROUPS ──────────────────────────────────────────────

    @GetMapping("/groups")
    public ResponseEntity<?> getGroups(@RequestParam(defaultValue = "") String q) {
        var filter = q.isEmpty() ? new Document()
                : regex("name", Pattern.compile(q, Pattern.CASE_INSENSITIVE));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document g : groups().find(filter)) result.add(docToMap(g));
        return ResponseEntity.ok(Map.of("groups", result));
    }

    @GetMapping("/groups/mine")
    public ResponseEntity<?> myGroups() {
        String uid = uid();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document g : groups().find(eq("members", uid))) result.add(docToMap(g));
        return ResponseEntity.ok(Map.of("groups", result));
    }

    @PostMapping("/groups/{gid}/join")
    public ResponseEntity<?> joinGroup(@PathVariable String gid) {
        String uid = uid();
        Document g = groups().find(eq("_id", new ObjectId(gid))).first();
        if (g == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

        List<String> members = (List<String>) g.getOrDefault("members", new ArrayList<>());
        if (members.contains(uid)) {
            groups().updateOne(eq("_id", new ObjectId(gid)), pull("members", uid));
            return ResponseEntity.ok(Map.of("joined", false));
        }
        groups().updateOne(eq("_id", new ObjectId(gid)), push("members", uid));
        return ResponseEntity.ok(Map.of("joined", true));
    }

    // ── GROUP CHAT ──────────────────────────────────────────

    @GetMapping("/groups/{gid}/chat")
    public ResponseEntity<?> getGroupChat(@PathVariable String gid) {
        String uid = uid();
        Document g = groups().find(eq("_id", new ObjectId(gid))).first();
        if (g == null) return ResponseEntity.status(404).body(Map.of("error", "Group not found"));

        List<String> members = (List<String>) g.getOrDefault("members", new ArrayList<>());
        if (!members.contains(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Join the group first"));

        Document chat = groupChats().find(eq("group_id", gid)).first();
        List<?> messages = (chat != null) ? (List<?>) chat.getOrDefault("messages", new ArrayList<>()) : new ArrayList<>();
        return ResponseEntity.ok(Map.of(
                "messages",    messages,
                "group_name",  g.getString("name"),
                "group_emoji", g.getOrDefault("emoji", "👥")
        ));
    }

    @PostMapping("/groups/{gid}/chat")
    public ResponseEntity<?> sendGroupMsg(@PathVariable String gid,
                                          @RequestBody Map<String, String> body) {
        String uid = uid();
        Document u = users().find(eq("_id", new ObjectId(uid))).first();
        Document g = groups().find(eq("_id", new ObjectId(gid))).first();
        if (g == null) return ResponseEntity.status(404).body(Map.of("error", "Group not found"));

        List<String> members = (List<String>) g.getOrDefault("members", new ArrayList<>());
        if (!members.contains(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Join the group first"));

        String text = body.getOrDefault("text", "").trim();
        if (text.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Empty message"));

        Document msg = new Document("msg_id",      new ObjectId().toHexString())
                .append("sender_id",   uid)
                .append("sender_name", u.getString("name"))
                .append("sender_dept", u.getOrDefault("department", ""))
                .append("text",        text)
                .append("created_at",  Instant.now().toString());

        groupChats().updateOne(
                eq("group_id", gid),
                combine(
                        push("messages", msg),
                        set("updated_at", Instant.now().toString()),
                        setOnInsert("group_id", gid)
                ),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
        );
        return ResponseEntity.status(201).body(Map.of("message", new LinkedHashMap<>(msg)));
    }

    // ── RESOURCES ───────────────────────────────────────────

    @GetMapping("/resources")
    public ResponseEntity<?> getResources(
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String q) {
        Document filter = new Document();
        if (!type.isEmpty()) filter.append("type", type);
        if (!q.isEmpty()) {
            Pattern p = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
            filter.append("$or", List.of(
                    new Document("title",   new Document("$regex", p)),
                    new Document("subject", new Document("$regex", p))
            ));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document r : resources().find(filter).sort(new Document("created_at", -1)))
            result.add(docToMap(r));
        return ResponseEntity.ok(Map.of("resources", result));
    }

    @PostMapping("/resources")
    public ResponseEntity<?> createResource(@RequestBody Map<String, Object> body) {
        String uid   = uid();
        Document u   = users().find(eq("_id", new ObjectId(uid))).first();
        String title   = ((String) body.getOrDefault("title",   "")).trim();
        String subject = ((String) body.getOrDefault("subject", "")).trim();
        if (title.isEmpty() || subject.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Title and subject required"));

        Document doc = new Document("title",         title)
                .append("subject",        subject)
                .append("type",           body.getOrDefault("type",  "notes"))
                .append("link",           ((String) body.getOrDefault("link", "")).trim())
                .append("tags",           body.getOrDefault("tags",  new ArrayList<>()))
                .append("uploader_id",    uid)
                .append("uploader_name",  u.getString("name"))
                .append("downloads",      0)
                .append("created_at",     Instant.now().toString());

        resources().insertOne(doc);
        return ResponseEntity.status(201).body(Map.of("resource", docToMap(doc)));
    }

    @PostMapping("/resources/{rid}/download")
    public ResponseEntity<?> downloadResource(@PathVariable String rid) {
        resources().updateOne(eq("_id", new ObjectId(rid)), inc("downloads", 1));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
