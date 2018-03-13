package org.jeduardo.entries.data;

import org.jeduardo.entries.model.Entry;
import org.springframework.data.repository.CrudRepository;

public interface EntryRepository extends CrudRepository<Entry, Integer> {
    
}
