package de.aspera.locapp.dao;

import java.util.Set;
import java.util.stream.Collectors;

import de.aspera.locapp.dto.IgnoredItem;

public class IgnoredItemDao extends AbstractDao<IgnoredItem> {
    public IgnoredItemDao() {
        super(IgnoredItem.class);
    }

    public Set<String> listIgnoredFiles() {
        return findAll()
            .stream()
            .map(item -> item.getFileName())
            .collect(Collectors.toSet());
    }
}
