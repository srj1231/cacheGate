package com.saumya.cachegate.cache;

/**
 * Cache entry containing a prompt, its embedding, and the corresponding response.
 * @param id
 * @param prompt
 * @param embedding
 * @param response
 */
public record CacheEntry(long id, String prompt, float[] embedding, String response) {}
