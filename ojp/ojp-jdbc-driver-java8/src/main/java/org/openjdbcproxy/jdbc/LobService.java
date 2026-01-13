package org.openjdbcproxy.jdbc;

import org.openjdbcproxy.grpc.LobDataBlock;
import org.openjdbcproxy.grpc.LobReference;
import org.openjdbcproxy.grpc.LobType;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

public interface LobService {
    LobReference sendBytes(LobType lobType, long pos, InputStream is) throws SQLException;
    LobReference sendBytes(LobType lobType, long pos, InputStream is, Map<Integer, Object> metadata) throws SQLException;
    InputStream parseReceivedBlocks(Iterator<LobDataBlock> itBlocks);
}
