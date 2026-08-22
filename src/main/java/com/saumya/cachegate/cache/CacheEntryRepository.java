package com.saumya.cachegate.cache;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * Repository for persisting and retrieving cache entries from a SQLite database.
 * Handles serialization/deserialization of embedding vectors between float arrays and byte arrays.
 *
 * SQLite only supports: INTEGER, REAL, TEXT, and BLOB.
 * BLOB is the suitable type for storing float arrays. It converts the float array to a byte array for storage.
 * - standard pattern for storing vector data in relational databases
 * - fast binary serialization/deserialization
 * - compact storage: 4 bytes per float
 * - preserves exact values
 * - no parsing overhead
 */
@Repository
public class CacheEntryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a new CacheEntryRepository.
     *
     * @param jdbcTemplate the JDBC template for database operations
     */
    public CacheEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves all cache entries from the database.
     *
     * @return a list of all cache entries with deserialized embeddings
     */
    public List<CacheEntry> findAll() {
        return jdbcTemplate.query(
                "SELECT id, prompt, embedding, response FROM cache_entries",
                (rs, rowNum) -> new CacheEntry(
                        rs.getLong("id"),
                        rs.getString("prompt"),
                        bytesToFloats(rs.getBytes("embedding")),
                        rs.getString("response")
                )
        );
    }

    /**
     * Inserts a new cache entry into the database using keyholder to get the auto-generated ID.
     *
     * @param prompt the prompt text
     * @param embedding the embedding vector
     * @param response the response text
     * @return the auto-generated ID of the new cache entry
     */
    public long save(String prompt, float[] embedding, String response) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO cache_entries (prompt, embedding, response) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, prompt);
            ps.setBytes(2, floatsToBytes(embedding));
            ps.setString(3, response);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Deletes all cache entries from the database.
     */
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM cache_entries");
    }

    /**
     * Converts a float array to a byte array for database storage.
     *
     * @param floats the float array to convert
     * @return the byte array representation
     * @throws RuntimeException if serialization fails
     */
    private byte[] floatsToBytes(float[] floats) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            for (float f : floats) dos.writeFloat(f);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
        return baos.toByteArray();
    }

    /**
     * Converts a byte array from the database back to a float array.
     *
     * @param bytes the byte array to convert
     * @return the float array representation
     * @throws RuntimeException if deserialization fails
     */
    private float[] bytesToFloats(byte[] bytes) {
        int count = bytes.length / 4;
        float[] floats = new float[count];
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            for (int i = 0; i < count; i++) floats[i] = dis.readFloat();
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize embedding", e);
        }
        return floats;
    }
}
