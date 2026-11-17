package com.inkwell.repository;

import com.inkwell.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByTitleContainingIgnoreCase(String keyword);
    List<Note> findByTitleContainingIgnoreCase(String keyword, Sort sort);
}
