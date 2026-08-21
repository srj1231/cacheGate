package com.saumya.cachegate.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Semantic cache for LLM responses using vector embeddings.
 * Stores prompt-response pairs with their embeddings and retrieves similar responses
 * based on cosine similarity of embeddings.
 */
@Component
public class SemanticCache {

    private static final double SIMILARITY_THRESHOLD = 0.8;
    private static final Logger log = LoggerFactory.getLogger(SemanticCache.class);
    private final List<CacheEntry> entries = new ArrayList<>();

    /**
     * Finds a cached response similar to the query embedding.
     *
     * @param queryEmbedding the embedding vector of the query prompt
     * @return an Optional containing the similar response if found above the similarity threshold
     */
    public synchronized Optional<String> findSimilar(float[] queryEmbedding) {
        CacheEntry best = null;
        double bestScore = 0.0;

        for(CacheEntry entry : entries){
            double score = cosineSimilarity(entry.embedding(), queryEmbedding);
            if(score >= SIMILARITY_THRESHOLD && score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }

        return best == null ? Optional.empty() : Optional.of(best.response());
    }

    /**
     * Stores a prompt-response pair with its embedding in the cache.
     *
     * @param prompt the input prompt
     * @param embeddings the embedding vector of the prompt
     * @param response the LLM response to cache
     */
    public synchronized void store(String prompt, float[] embeddings, String response) {
        entries.add(new CacheEntry(prompt, embeddings, response));
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
}
