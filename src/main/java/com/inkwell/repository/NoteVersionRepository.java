package com.inkwell.repository;

import com.inkwell.model.NoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteVersionRepository
        extends JpaRepository<NoteVersion, Long> {

    @Query("SELECT v FROM NoteVersion v " +
            "WHERE v.note.id = :noteId " +
            "ORDER BY v.savedAt DESC")
    List<NoteVersion> findVersionsByNoteId(
            @Param("noteId") Long noteId);
}