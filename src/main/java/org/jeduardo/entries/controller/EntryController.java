package org.jeduardo.entries.controller;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jeduardo.entries.data.EntryRepository;
import org.jeduardo.entries.exception.ResourceNotFoundException;
import org.jeduardo.entries.model.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class EntryController {
    private final static Logger LOGGER = LogManager.getLogger(EntryController.class);

    @Autowired
    private EntryRepository entryRepository;

    @RequestMapping(value = "/api/v1/status", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String status() {
        return "OK";
    }

    @RequestMapping(value = "/api/v1/entries", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<Entry> list() {
        return entryRepository.findAll();
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Entry list(@PathVariable("id") int id) {
        Entry entry = entryRepository.findOne(id);
        if (entry != null) {
            LOGGER.info(String.format("Entry found: %s", entry));
            return entry;
        } else {
            LOGGER.info(String.format("Entry not found for id: %d", entry));
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
    public Entry update(@PathVariable("id") int id, @RequestBody Entry entry) {
        Entry targetEntry = entryRepository.findOne(id);
        if (targetEntry != null) {
            LOGGER.info(String.format("Entry found: %s", entry));
            targetEntry.setContent(entry.getContent());
            targetEntry.setDescription(entry.getDescription());
            return entryRepository.save(targetEntry);
        } else {
            LOGGER.info(String.format("Entry not found for id: %d", id));
            throw new ResourceNotFoundException("Entry not found");
        }
    }

    @RequestMapping(value = "/api/v1/entries/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Entry delete(@PathVariable("id") int id) {
        Entry targetEntry = entryRepository.findOne(id);
        if (targetEntry != null) {
            entryRepository.delete(targetEntry);
            return targetEntry;
        } else {
            LOGGER.info(String.format("Entry not found for id: %d", id));
            throw new ResourceNotFoundException("Entry not found");
        }
    }
}
