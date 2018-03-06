package org.jeduardo.entries.model;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.persistence.*;

@Entity
@Table(name = "entries")
public class Entry {
    @Id
    @SequenceGenerator(name="entries_id_seq", sequenceName="entries_id_seq")
    @GeneratedValue(generator="entries_id_seq")
    private int id = 0;
    private String content = null;
    private String description = null;

    // Required for deserialization of incoming parameters
    public Entry() {}

    public Entry(int id, String content, String description) {
        this.id = id;
        this.content = content;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getDescription() {
        return description;
    }

    public void setId(int id) {
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
