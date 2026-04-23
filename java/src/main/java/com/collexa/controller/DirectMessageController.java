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

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@RestController
@RequestMapping("/api/dm")
public class DirectMessageController {

    private final MongoDatabase db;

    public DirectMessageController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> dms()   { return db.getCollection("direct_messages"); }
    private MongoCollection<Document> users() { return db.getCollection("users"); }

    private String uid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Document getOrCreateConv(String a, String b) {
        List<String> parts = new ArrayList<>(Arrays.asList(a, b));
        Collections.sort(parts);
        Document c = dms().find(eq("participants", parts)).first();
        if (c == null) {
            Document doc = new Document("participants", parts)
                    .append("messages", new ArrayList<>())
                    .append("updated_at", Instant.now().toString());
            dms().insertOne(doc);
            return doc;
        }
        return c;
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        String uid = uid();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Document c : dms().find(eq("participants", uid))
                .sort(new Document("updated_at", -1))) {
            List<String> participants = (List<String>) c.get("participants");
            String otherId = participants.stream().filter(p -> !p.equals(uid)).findFirst().orElse(null);
            if (otherId == null) continue;

            Document other;
            try { other = users().find(eq("_id", new ObjectId(otherId))).first(); }
            catch (Exception e) { continue; }
            if (other == null) continue;

            List<Document> msgs = (List<Document>) c.getOrDefault("messages", new ArrayList<>());
            Document last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            long unread = msgs.stream()
                    .filter(m -> !uid.equals(m.get("sender_id")) && !Boolean.TRUE.equals(m.get("read")))
                    .count();

            String dept = other.getString("department") != null ? other.getString("department") : "";
            String year = other.getString("year") != null ? other.getString("year") : "";
            String otherDept = (dept + " · " + year).replaceAll("^\\s*·\\s*|\\s*·\\s*$", "");

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("conv_id",      c.getObjectId("_id").toHexString());
            entry.put("other_id",     otherId);
            entry.put("other_name",   other.getOrDefault("name", ""));
            entry.put("other_dept",   otherDept);
            entry.put("last_message", last != null ? last.getOrDefault("text", "") : "");
            entry.put("last_time",    last != null ? last.getOrDefault("created_at", "") : "");
            entry.put("unread",       unread);
            result.add(entry);
        }
        return ResponseEntity.ok(Map.of("conversations", result));
    }

    @PostMapping("/start/{otherId}")
    public ResponseEntity<?> startConv(@PathVariable String otherId) {
        String uid = uid();
        if (uid.equals(otherId)) return ResponseEntity.badRequest().body(Map.of("error", "Cannot message yourself"));
        if (users().find(eq("_id", new ObjectId(otherId))).first() == null)
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        Document c = getOrCreateConv(uid, otherId);
        return ResponseEntity.ok(Map.of("conv_id", c.getObjectId("_id").toHexString()));
    }

    @GetMapping("/{cid}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String cid) {
        String uid = uid();
        Document c = dms().find(eq("_id", new ObjectId(cid))).first();
        if (c == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

        List<String> participants = (List<String>) c.get("participants");
        if (!participants.contains(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));

        // Mark messages from other as read
        dms().updateOne(
                eq("_id", new ObjectId(cid)),
                new Document("$set", new Document("messages.$[e].read", true)),
                new com.mongodb.client.model.UpdateOptions()
                        .arrayFilters(List.of(new Document("e.sender_id", new Document("$ne", uid))))
        );

        List<?> messages = (List<?>) c.getOrDefault("messages", new ArrayList<>());
        return ResponseEntity.ok(Map.of("messages", messages));
    }

    @PostMapping("/{cid}/send")
    public ResponseEntity<?> sendMessage(@PathVariable String cid,
                                         @RequestBody Map<String, String> body) {
        String uid = uid();
        Document u = users().find(eq("_id", new ObjectId(uid))).first();
        Document c = dms().find(eq("_id", new ObjectId(cid))).first();
        if (c == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

        List<String> participants = (List<String>) c.get("participants");
        if (!participants.contains(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));

        String text = body.getOrDefault("text", "").trim();
        if (text.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Empty message"));

        Document msg = new Document("msg_id",      new ObjectId().toHexString())
                .append("sender_id",   uid)
                .append("sender_name", u.getString("name"))
                .append("text",        text)
                .append("read",        false)
                .append("created_at",  Instant.now().toString());

        dms().updateOne(eq("_id", new ObjectId(cid)),
                combine(push("messages", msg), set("updated_at", Instant.now().toString())));

        return ResponseEntity.status(201).body(Map.of("message", new LinkedHashMap<>(msg)));
    }
}
