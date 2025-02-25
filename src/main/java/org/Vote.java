package org;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Vote implements Serializable {
    private static final long serialVersionUID = 1L; // Уникальный идентификатор версии

    private String name;
    private String description;
    private List<String> options;
    private String creator;
    private Map<String, Integer> results;

    public Vote(String name, String description, List<String> options, String creator) {
        this.name = name;
        this.description = description;
        this.options = options;
        this.creator = creator;
        this.results = new HashMap<>();
        for (String option : options) {
            results.put(option, 0);
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCreator() {
        return creator;
    }

    public Map<String, Integer> getResults() {
        return results;
    }

    public void vote(String option) {
        results.put(option, results.getOrDefault(option, 0) + 1);
    }
}