
package com.inkwell.controller;

import com.inkwell.model.Note;
import com.inkwell.repository.NoteRepository;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final NoteRepository noteRepository;

    public HomeController(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute("appName", "Inkwell");
        model.addAttribute("tagline",
                "A quiet space for your thoughts.");

        // Retrieve five recently updated notes
        List<Note> recentNotes = noteRepository.findAll(
                Sort.by(Sort.Direction.DESC, "updatedAt")
        ).stream().limit(5).toList();

        // Convert HTML content into plain-text previews
        Map<Long, String> recentNotePreviews = new HashMap<>();

        for (Note note : recentNotes) {

            String content = note.getContent() == null
                    ? ""
                    : note.getContent();

            String plainText = Jsoup.parse(content).text();

            recentNotePreviews.put(note.getId(), plainText);
        }

        model.addAttribute("recentNotes", recentNotes);
        model.addAttribute("recentNotePreviews", recentNotePreviews);

        return "dashboard";
    }
}
