package com.specomega.openprotege.server.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministratorBootstrapTest {
    @Test
    void refusesToStartWithEmptyDatabaseAndNoBootstrapCredentials() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ApplicationArguments arguments = mock(ApplicationArguments.class);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).thenReturn(0);

        AdministratorBootstrap bootstrap =
                new AdministratorBootstrap(jdbcTemplate, passwordEncoder, "", "");

        assertThatThrownBy(() -> bootstrap.run(arguments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires explicit administrator bootstrap credentials");
        verify(jdbcTemplate, never()).update(any(String.class), any(Object[].class));
    }
}
