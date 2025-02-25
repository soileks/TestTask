package org;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VoteCreationState implements Serializable {
    private static final long serialVersionUID = 1L; // Уникальный идентификатор версии

    private String topicName;
    private String voteName;
    private String voteDescription;
    private int optionsCount;
    private List<String> options;
    private boolean isVoting;

    public VoteCreationState() {
        this.options = new ArrayList<>();
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getVoteName() {
        return voteName;
    }

    public void setVoteName(String voteName) {
        this.voteName = voteName;
    }

    public String getVoteDescription() {
        return voteDescription;
    }

    public void setVoteDescription(String voteDescription) {
        this.voteDescription = voteDescription;
    }

    public int getOptionsCount() {
        return optionsCount;
    }

    public void setOptionsCount(int optionsCount) {
        this.optionsCount = optionsCount;
    }

    public List<String> getOptions() {
        return options;
    }

    public void addOption(String option) {
        options.add(option);
    }

    public boolean isVoting() {
        return isVoting;
    }

    public void setVoting(boolean voting) {
        isVoting = voting;
    }
}