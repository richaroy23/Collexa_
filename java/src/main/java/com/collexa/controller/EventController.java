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

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final MongoDatabase db;

    public EventController(MongoDatabase db) {
        this.db = db;
    }

    private MongoCollection<Document> events() { return db.getCollection("events"); }

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
    public ResponseEntity<?> getEvents(@RequestParam(defaultValue = "") String q) {
        var filter = q.isEmpty() ? new Document()
                : regex("title", Pattern.compile(q, Pattern.CASE_INSENSITIVE));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document e : events().find(filter).sort(new Document("date", 1))) {
            result.add(docToMap(e));
        }
        return ResponseEntity.ok(Map.of("events", result));
    }

    @PostMapping("/{eid}/rsvp")
    public ResponseEntity<?> rsvp(@PathVariable String eid) {
        String uid = uid();
        Document ev = events().find(eq("_id", new ObjectId(eid))).first();
        if (ev == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

        List<String> att = (List<String>) ev.getOrDefault("attendees", new ArrayList<>());
        if (att.contains(uid)) {
            events().updateOne(eq("_id", new ObjectId(eid)), pull("attendees", uid));
            return ResponseEntity.ok(Map.of("rsvpd", false));
        }
        events().updateOne(eq("_id", new ObjectId(eid)), push("attendees", uid));
        return ResponseEntity.ok(Map.of("rsvpd", true));
    }
}
