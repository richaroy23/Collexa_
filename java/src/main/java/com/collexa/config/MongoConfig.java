package com.collexa.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String dbName;

    /**
     * Creates a MongoClient configured for MongoDB Atlas (or local).
     * The URI in .env should be an Atlas SRV string:
     *   mongodb+srv://<user>:<pass>@cluster.mongodb.net/collexa?retryWrites=true&w=majority
     */
    @Bean
    public MongoClient mongoClient() {
        log.info("Connecting to MongoDB – database: {}", dbName);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                // Atlas heartbeat / server-selection timeouts (ms)
                .applyToClusterSettings(b -> b
                        .serverSelectionTimeout(10_000, TimeUnit.MILLISECONDS))
                // Socket-level timeouts to avoid hanging on flaky network
                .applyToSocketSettings(b -> b
                        .connectTimeout(10_000, TimeUnit.MILLISECONDS)
                        .readTimeout(30_000, TimeUnit.MILLISECONDS))
                // Connection-pool tuning (safe defaults for a small app)
                .applyToConnectionPoolSettings(b -> b
                        .maxSize(20)
                        .minSize(2)
                        .maxWaitTime(5_000, TimeUnit.MILLISECONDS))
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        MongoDatabase db = mongoClient.getDatabase(dbName);

        // Ensure unique index on email and a compound index on DMs
        try {
            db.getCollection("users").createIndex(
                    new Document("email", 1),
                    new com.mongodb.client.model.IndexOptions().unique(true)
            );
            db.getCollection("direct_messages").createIndex(
                    new Document("participants", 1)
            );
            log.info("MongoDB indexes verified.");
        } catch (Exception e) {
            log.warn("Index creation skipped (may already exist): {}", e.getMessage());
        }

        return db;
    }
}
