package org.openjdbcproxy.grpc;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import org.openjdbcproxy.grpc.dto.Parameter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;



/**
 * Handles serialization of java objects to and from byte arrays.
 */
public class SerializationHandler {
    public static byte[] serialize(Object t) {
        try (ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            try (ObjectOutputStream so = new ObjectOutputStream(bo)) {
                so.writeObject(t);
                so.flush();
                return bo.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(byte[] byteArray, Class<T> type) {
        if (byteArray == null || byteArray.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bi = new ByteArrayInputStream(byteArray)) {
            try (ObjectInputStream si = new ObjectInputStream(bi)) {
                return type.cast(si.readObject());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String deserializeParams(ByteString params) {
        // 使用fastjson序列化参数为JSON字符串
        String parametersStr = "[]";
        if (!params.isEmpty()) {
            try {
                List<Parameter> parameters = SerializationHandler.deserialize(
                        params.toByteArray(), List.class);
                parametersStr = parameters != null ? JSON.toJSONString(parameters) : "[]";
            } catch (Exception e) {
                parametersStr = "JSON serialization failed: " + e.getMessage();
            }
        }
        return parametersStr;
    }
}
