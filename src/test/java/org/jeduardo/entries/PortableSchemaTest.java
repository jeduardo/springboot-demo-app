package org.jeduardo.entries;

import static org.assertj.core.api.Assertions.assertThat;

import org.jeduardo.entries.data.EntryRepository;
import org.jeduardo.entries.model.Entry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PortableSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntryRepository repository;

    @Test
    void flywayCreatesBigintIdsAndAnAllocatorTable() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'ENTRIES' AND COLUMN_NAME = 'ID'
                """, String.class)).isEqualTo("BIGINT");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT next_id
                FROM entry_id_generator
                WHERE sequence_name = 'entries'
                """, Long.class)).isZero();

        Entry entry = repository.save(new Entry());
        assertThat(entry.getId()).isEqualTo(1L);
    }
}
