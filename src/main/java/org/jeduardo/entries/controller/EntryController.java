package org.jeduardo.entries.controller;

import org.jeduardo.entries.data.EntryRepository;
import org.jeduardo.entries.exception.ResourceNotFoundException;
import org.jeduardo.entries.model.Entry;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class EntryController {

    @Autowired
    private EntryRepository entryRepository;

    @RequestMapping(value = "/api/v1/status", method = RequestMethod.GET, produces = "application/json")
    public String status() {
        return JSONObject.quote("OK");
    }

    @RequestMapping(value = "/api/v1/entries", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<Entry> list() {
        return entryRepository.findAll();
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Entry list(@PathVariable("id") long id) {
        Entry entry = entryRepository.findOne(id);
        if (entry != null) {
            return entry;
        } else {
            throw new ResourceNotFoundException("Entry not found");
        }
    }

    @RequestMapping(value = "/api/v1/entries", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Entry create(@RequestBody Entry entry) {
        Entry newEntry = new Entry();
        newEntry.setContent(entry.getContent());
        newEntry.setDescription(entry.getDescription());
        return entryRepository.save(newEntry);
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Entry update(@PathVariable("id") long id, @RequestBody Entry entry) {
        Entry targetEntry = entryRepository.findOne(id);
        if (targetEntry != null) {
            targetEntry.setContent(entry.getContent());
            targetEntry.setDescription(entry.getDescription());
            return entryRepository.save(targetEntry);
        } else {
            throw new ResourceNotFoundException("Entry not found");
        }
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Entry delete(@PathVariable("id") long id) {
        Entry targetEntry = entryRepository.findOne(id);
        if (targetEntry != null) {
            entryRepository.delete(targetEntry);
            return targetEntry;
        } else {
            throw new ResourceNotFoundException("Entry not found");
        }
    }
}
