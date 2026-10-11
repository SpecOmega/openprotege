package com.specomega.openprotege.server.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class JdbcUuid {
    private JdbcUuid() {}

    public static UUID read(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? null : UUID.fromString(value);
    }
}
