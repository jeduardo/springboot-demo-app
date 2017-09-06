package org.jeduardo.entries.controller;

import org.jeduardo.entries.exception.ResourceNotFoundException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.jeduardo.entries.model.Entry;

import java.util.ArrayList;
import java.util.List;

@RestController
public class EntryController {

    private static List<Entry> entries = new ArrayList<>();

    @RequestMapping(value = "/api/v1/status", method = RequestMethod.GET, produces = "application/json")
    public String status() {
        return JSONObject.quote("OK");
    }

    @RequestMapping(value = "/api/v1/entries", method = RequestMethod.GET, produces = "application/json")
    public List<Entry> list() {
        return entries;
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.GET, produces = "application/json")
    public Entry list(@PathVariable("id") long id) {
        if (entries.size() >= id) {
            return entries.get((int) id);
        } else {
            throw new ResourceNotFoundException("Entry not found");
        }
    }

    @RequestMapping(value = "/api/v1/entries", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Entry add(@RequestBody Entry entry) {
        Entry newEntry = new Entry(entries.size() + 1, entry.getContent(), entry.getDescription());
        entries.add(newEntry);
        return newEntry;
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Entry update(@PathVariable("id") long id, @RequestBody Entry entry) {
        if (entries.size() >= id) {
            Entry targetEntry = entries.get((int) id - 1);
            targetEntry.setContent(entry.getContent());
            targetEntry.setDescription(entry.getDescription());
            return targetEntry;
        } else {
            throw new ResourceNotFoundException("Entry not found");
        }
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.DELETE, produces = "application/json")
    public Entry update(@PathVariable("id") long id) {
        if (entries.size() >= id) {
            return entries.remove((int) id - 1);
        } else {
            throw new ResourceNotFoundException("Entry not found");
        }
    }
}
