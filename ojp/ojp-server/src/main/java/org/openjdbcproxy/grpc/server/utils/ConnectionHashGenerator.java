package org.openjdbcproxy.grpc.server.utils;

import com.openjdbcproxy.grpc.ConnectionDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.openjdbcproxy.grpc.server.Constants.SHA_256;

/**
 * Utility class for generating connection hashes.
 * Extracted from StatementServiceImpl to improve modularity.
 */
public class ConnectionHashGenerator {

    /**
     * Generates a hash for connection details using SHA-256.
     *
     * @param connectionDetails The connection details to hash
     * @return Hash string for the connection details
     * @throws RuntimeException if hashing fails
     */
    public static String hashConnectionDetails(ConnectionDetails connectionDetails) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);
            messageDigest.update((connectionDetails.getUrl() + connectionDetails.getUser() + connectionDetails.getPassword())
                    .getBytes(StandardCharsets.UTF_8));
            byte[] hashBytes = messageDigest.digest();
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate connection hash: SHA-256 algorithm not available", e);
        }
    }
}