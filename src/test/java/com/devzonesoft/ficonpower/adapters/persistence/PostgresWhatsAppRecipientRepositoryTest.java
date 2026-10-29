package com.devzonesoft.ficonpower.adapters.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PostgresWhatsAppRecipientRepositoryTest {

    @Test
    void listsGlobalAndUserLinkedRecipientsTogether() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new PostgresWhatsAppRecipientRepository(jdbc).findAll();

        verify(jdbc).query(
                argThat((String sql) -> sql.contains("FROM whatsapp_recipients r")
                        && !sql.contains("r.user_id IS NULL")
                        && sql.contains("r.user_id NULLS FIRST")),
                any(RowMapper.class));
    }
}
