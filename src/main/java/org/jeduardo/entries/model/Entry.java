package org.jeduardo.entries.model;

import javax.persistence.*;

@Entity
@Table(name = "entries")
public class Entry {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id = 0;
    private String content = null;
    private String description = null;

    // Required for deserialization of incoming parameters
    public Entry() {}

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
}
