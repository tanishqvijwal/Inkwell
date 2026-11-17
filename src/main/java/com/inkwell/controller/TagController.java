package com.inkwell.controller;

import com.inkwell.model.Note;
import org.jsoup.Jsoup;
import java.util.HashMap;
import java.util.Map;

import com.inkwell.model.Tag;
import com.inkwell.repository.TagRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tags")
public class TagController {

    private final TagRepository tagRepository;

    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @GetMapping
    public String getAllTags(Model model) {
        model.addAttribute("tags", tagRepository.findAll());
        return "tags/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("tag", new Tag());
        return "tags/form";
    }

    @PostMapping("/save")
    public String saveTag(@ModelAttribute("tag") Tag tag) {
        tagRepository.save(tag);
        return "redirect:/tags";
    }

    @PostMapping("/delete/{id}")
    public String deleteTag(@PathVariable Long id) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid tag ID"));

        tag.getNotes().forEach(note -> note.getTags().remove(tag));

        tagRepository.delete(tag);

        return "redirect:/tags";
    }
    @GetMapping("/{id}")
    public String viewTag(@PathVariable Long id, Model model) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid tag ID"));

        // Generate plain-text previews
        Map<Long, String> notePreviews = new HashMap<>();

        for (Note note : tag.getNotes()) {

            String content = note.getContent() == null
                    ? ""
                    : note.getContent();

            notePreviews.put(
                    note.getId(),
                    Jsoup.parse(content).text()
            );
        }

        model.addAttribute("tag", tag);
        model.addAttribute("notes", tag.getNotes());
        model.addAttribute("notePreviews", notePreviews);

        return "tags/view";
    }
}