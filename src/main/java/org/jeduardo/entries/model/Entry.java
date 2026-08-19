package org.jeduardo.entries.model;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name = "entries")
public class Entry {
    @Id
    @TableGenerator(
            name = "entry_id_generator",
            table = "entry_id_generator",
            pkColumnName = "sequence_name",
            valueColumnName = "next_id",
            pkColumnValue = "entries",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "entry_id_generator")
    private long id = 0;
    private String content = null;
    private String description = null;

    // Required for deserialization of incoming parameters
    public Entry() {
    }

    public Entry(long id, String content, String description) {
        this.id = id;
        this.content = content;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getDescription() {
        return description;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
