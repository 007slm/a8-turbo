package org.openjdbcproxy.cache.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RuleType {
        TABLES_ANY("tablesAny"),
        TABLES_ALL("tablesAll"),
        QUERY_IDS("queryIds"),
        REGEX("regex");

        private final String value;

        RuleType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static RuleType fromValue(String value) {
            if (value == null) {
                return null;
            }
            
            for (RuleType type : RuleType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            
            // 兼容旧的值
            if ("tablesAny".equalsIgnoreCase(value)) {
                return TABLES_ANY;
            }
            
            throw new IllegalArgumentException("Unknown RuleType value: " + value);
        }

        @Override
        public String toString() {
            return value;
        }
    }