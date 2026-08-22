package com.saumya.cachegate.cache;

/**
 * A cache entry with a similarity score.
 * @param entry
 * @param score
 */
public record ScoredEntry(CacheEntry entry, double score) { }
