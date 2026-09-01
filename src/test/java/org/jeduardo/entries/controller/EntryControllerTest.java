package org.jeduardo.entries.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.jeduardo.entries.Application;
import org.jeduardo.entries.data.EntryRepository;
import org.jeduardo.entries.model.Entry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
public class EntryControllerTest {

        @Autowired
        private MockMvc mvc;

        @Autowired
        private EntryRepository repository;

        @AfterEach
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
        public void createsEntryWhenRequestOmitsId() throws Exception {
                MvcResult result = mvc.perform(post("/api/v1/entries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"content":"created","description":"first"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.content", is("created")))
                                .andExpect(jsonPath("$.description", is("first")))
                                .andReturn();

                Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
                assertThat(repository.findById(id.longValue()))
                                .hasValueSatisfying(entry -> {
                                        assertThat(entry.getContent()).isEqualTo("created");
                                        assertThat(entry.getDescription()).isEqualTo("first");
                                });
        }

        @Test
        public void updatesExistingEntryWhenRequestOmitsId() throws Exception {
                Entry existing = new Entry();
                existing.setContent("before");
                existing.setDescription("original");
                existing = repository.save(existing);

                MvcResult result = mvc.perform(post("/api/v1/entries/" + existing.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"content":"updated","description":"second"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.content", is("updated")))
                                .andExpect(jsonPath("$.description", is("second")))
                                .andReturn();

                Number returnedId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
                assertThat(returnedId.longValue()).isEqualTo(existing.getId());
                assertThat(repository.findById(existing.getId()))
                                .hasValueSatisfying(entry -> {
                                        assertThat(entry.getContent()).isEqualTo("updated");
                                        assertThat(entry.getDescription()).isEqualTo("second");
                                });
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
