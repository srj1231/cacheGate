package com.saumya.cachegate.cache;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final CacheEntryRepository repository;
    private final List<CacheEntry> entries = new ArrayList<>();

    public SemanticCache(CacheEntryRepository repository) {
        this.repository = repository;
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
     * Finds a cached response similar to the query embedding.
     *
     * @param queryEmbedding the embedding vector of the query prompt
     * @return an Optional containing the similar response if found above the similarity threshold
     */
    public synchronized Optional<String> findSimilar(float[] queryEmbedding) {
        totalChecks.incrementAndGet();
        CacheEntry best = null;
        double bestScore = 0.0;

        for(CacheEntry entry : entries){
            double score = cosineSimilarity(entry.embedding(), queryEmbedding);
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }

        if (best == null) {
            log.info("SIMILARITY CHECK: cache is empty, nothing to compare against");
            return Optional.empty();
        }

        log.info("SIMILARITY CHECK: closest match scored {} against cached prompt \"{}\"", bestScore, best.prompt());
        if (bestScore >= SIMILARITY_THRESHOLD) {
            hits.incrementAndGet();
            return Optional.of(best.response());
        }
        return Optional.empty();
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
