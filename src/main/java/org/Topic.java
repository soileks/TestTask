package org;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Topic implements Serializable {
    private static final long serialVersionUID = 1L; // Уникальный идентификатор версии

    private String name;
    private List<Vote> votes;

    public Topic(String name) {
        this.name = name;
        this.votes = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void addVote(Vote vote) {
        votes.add(vote);
    }

    public void removeVote(Vote vote) {
        votes.remove(vote);
    }
}