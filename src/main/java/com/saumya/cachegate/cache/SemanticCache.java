package com.saumya.cachegate.cache;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Semantic cache for LLM responses using vector embeddings.
 * Stores prompt-response pairs with their embeddings and retrieves similar responses
 * based on cosine similarity of embeddings.
 */
@Component
public class SemanticCache {

    private final AtomicInteger totalChecks = new AtomicInteger(0);
    private final AtomicInteger hits = new AtomicInteger(0);

    private static final double SIMILARITY_THRESHOLD = 0.8;
    private static final Logger log = LoggerFactory.getLogger(SemanticCache.class);
    private static final int MAX_CANDIDATES = 3;

    private final CacheEntryRepository repository;
    private final List<CacheEntry> entries = new ArrayList<>();

    private final double autoAcceptThreshold;
    private final double candidateFloor;

    public SemanticCache(CacheEntryRepository repository,
                         @Value("${cachegate.cache.auto-accept-threshold:0.95}") double autoAcceptThreshold,
                         @Value("${cachegate.cache.candidate-floor:0.75}") double candidateFloor) {
        this.repository = repository;
        this.autoAcceptThreshold = autoAcceptThreshold;
        this.candidateFloor = candidateFloor;
    }

    /**
     * Loads all cache entries from the database into memory.
     */
    @PostConstruct
    public void loadFromDisk() {
        entries.addAll(repository.findAll());
        log.info("Loaded {} cache entries from disk", entries.size());
    }

    /**
     * Finds cached candidates similar to the query embedding.
     *
     * @param queryEmbedding the embedding vector of the query prompt
     * @return a list of scored entries that are similar to the query
     */
    public synchronized List<ScoredEntry> findCandidates(float[] queryEmbedding) {
        totalChecks.incrementAndGet();
        List<ScoredEntry> scored = new ArrayList<>();
        for (CacheEntry entry : entries) {
            double score = cosineSimilarity(entry.embedding(), queryEmbedding);
            log.info("Cosine similarity: {} for cached prompt \"{}\"", score, entry.prompt());
            if (score >= candidateFloor) {
                scored.add(new ScoredEntry(entry, score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scored.size() > MAX_CANDIDATES ? scored.subList(0, MAX_CANDIDATES) : scored;
    }

    public boolean isAutoAccept(double score) {
        return score >= autoAcceptThreshold;
    }

    /**
     * Find a cached entry by its ID.
     */
    public synchronized Optional<CacheEntry> findById(long id) {
        return entries.stream().filter(e -> e.id() == id).findFirst();
    }

    /**
     * Stores a new cache entry in memory and database.
     */
    public synchronized void store(String prompt, float[] embedding, String response) {
        long id = repository.save(prompt, embedding, response);
        entries.add(new CacheEntry(id, prompt, embedding, response));
        log.info("STORED new cache entry id={} for prompt: \"{}\"", id, prompt);
    }

    /**
     * Record a cache hit and return the current hit count.
     */
    public int recordHit() {
        return hits.incrementAndGet();
    }

    /**
     * Clear all cached entries and reset hit-rate stats.
     */
    public synchronized void clear() {
        entries.clear();
        repository.deleteAll();
        totalChecks.set(0);
        hits.set(0);
        log.info("CACHE CLEARED — all entries removed, stats reset");
    }

    /**
     * Calculates the cosine similarity between two embedding vectors.
     *
     * @param a the first embedding vector
     * @param b the second embedding vector
     * @return the cosine similarity score between 0.0 and 1.0
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;

        for(int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if(normA == 0 || normB == 0) {
            return 0.0;
        }

        var similarityScore = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        log.info("Cosine similarity: {}", similarityScore);
        return similarityScore;
    }

    public int getTotalChecks() { return totalChecks.get(); }
    public int getHits() { return hits.get(); }
    public int getEntryCount() { return entries.size(); }
}
