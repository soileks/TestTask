package org;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // Уникальный идентификатор версии

    private String username;

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}