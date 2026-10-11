package com.specomega.openprotege.server.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class JdbcConfiguration {
    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource, DatabaseDialect databaseDialect) {
        return new JdbcTemplate(dataSource) {
            @Override
            protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
                if (!databaseDialect.isMysql()) {
                    return super.newArgPreparedStatementSetter(args);
                }
                Object[] converted = args.clone();
                for (int index = 0; index < converted.length; index++) {
                    if (converted[index] instanceof UUID uuid) {
                        converted[index] = uuid.toString();
                    }
                }
                return super.newArgPreparedStatementSetter(converted);
            }
        };
    }
}
