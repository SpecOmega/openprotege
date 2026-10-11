package com.specomega.openprotege.server.database;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseDialect {
    private final boolean mysql;

    public DatabaseDialect(Environment environment) {
        this.mysql = environment.acceptsProfiles(Profiles.of("mysql"));
    }

    public boolean isMysql() {
        return mysql;
    }

    public Object uuidParameter(UUID value) {
        return mysql && value != null ? value.toString() : value;
    }
}
