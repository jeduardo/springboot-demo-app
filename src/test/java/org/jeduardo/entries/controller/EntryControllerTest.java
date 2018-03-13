package org.jeduardo.entries.controller;

import org.jeduardo.entries.Application;
import org.jeduardo.entries.data.EntryRepository;
import org.jeduardo.entries.model.Entry;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
public class EntryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntryRepository repository;

    @After
    public void resetDb() {
        repository.deleteAll();
    }

    @Test
    public void getEntriesAndReceiveEmptyList() throws Exception {

        mvc.perform(get("/api/v1/entries")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("[]"));
    }

    @Test
    public void getEntriesAndReceiveListWithItems() throws Exception {
        Entry entry1 = new Entry();
        entry1.setContent("Content 1");
        entry1.setDescription("Description 1");
        repository.save(entry1);

        Entry entry2 = new Entry();
        entry2.setContent("Content 2");
        entry2.setDescription("Description 2");
        repository.save(entry2);

        mvc.perform(get("/api/v1/entries")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].content", is("Content 1")))
                .andExpect(jsonPath("$[0].description", is("Description 1")))
                .andExpect(jsonPath("$[1].content", is("Content 2")))
                .andExpect(jsonPath("$[1].description", is("Description 2")));

    }

    @Test
    public void getEntryById() throws Exception {
        Entry entry1 = new Entry();
        entry1.setContent("Content 1");
        entry1.setDescription("Description 1");
        entry1 = repository.save(entry1);

        mvc.perform(get("/api/v1/entries/" + entry1.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", is("Content 1")))
                .andExpect(jsonPath("$.description", is("Description 1")));

    }

    @Test
    public void deleteEntry() throws Exception {
        Entry entry1 = new Entry();
        entry1.setContent("Content 1");
        entry1.setDescription("Description 1");
        entry1 = repository.save(entry1);

        mvc.perform(delete("/api/v1/entries/" + entry1.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", is("Content 1")))
                .andExpect(jsonPath("$.description", is("Description 1")));

        mvc.perform(get("/api/v1/entries/" + entry1.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

    }
}