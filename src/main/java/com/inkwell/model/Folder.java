package com.inkwell.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folders")
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @OneToMany(mappedBy = "folder")
    private List<Note> notes = new ArrayList<>();

    public Folder() {
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public Folder(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Note> getNotes() {
        return notes;
    }
}
