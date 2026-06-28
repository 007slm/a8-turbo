package org.openjdbcproxy.grpc.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Common constants used across the server implementation.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    /**
     * Empty list constant
     */
    public static final List<?> EMPTY_LIST = Collections.emptyList();

    /**
     * Empty map constant
     */
    public static final Map<?, ?> EMPTY_MAP = Collections.emptyMap();

    /**
     * Empty string constant
     */
    public static final String EMPTY_STRING = "";

    /**
     * SHA-256 algorithm name
     */
    public static final String SHA_256 = "SHA-256";

    /**
     * Transaction dirty flag key
     */
    public static final String TRX_IS_DIRTY = "trx.is.dirty";
}
