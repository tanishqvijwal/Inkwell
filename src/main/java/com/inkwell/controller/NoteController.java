package com.inkwell.controller;

// pdf download
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.inkwell.model.Folder;
import com.inkwell.model.Note;
import com.inkwell.repository.FolderRepository;
import com.inkwell.repository.NoteRepository;
import com.inkwell.model.Tag;
import com.inkwell.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import com.inkwell.model.NoteVersion;
import com.inkwell.repository.NoteVersionRepository;
import java.time.LocalDateTime;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/notes")
public class NoteController {

    private final NoteVersionRepository noteVersionRepository;
    private final TagRepository tagRepository;
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;

    public NoteController(NoteVersionRepository noteVersionRepository, TagRepository tagRepository, NoteRepository noteRepository,
                          FolderRepository folderRepository) {
        this.noteVersionRepository = noteVersionRepository;
        this.tagRepository = tagRepository;
        this.noteRepository = noteRepository;
        this.folderRepository = folderRepository;
    }


    @GetMapping("/export/{id}")
    public ResponseEntity<byte[]> exportNoteAsPdf(@PathVariable Long id) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid note ID"));

        try {

            // Create an HTML document for PDF generation
            Document pdfDocument = Jsoup.parse(
                    "<!DOCTYPE html>" +
                            "<html><head>" +
                            "<meta charset=\"UTF-8\"/>" +
                            "<style>" +

                            "@page { size: A4; margin: 22mm; }" +

                            "body {" +
                            "font-family: sans-serif;" +
                            "color: #39352f;" +
                            "line-height: 1.7;" +
                            "font-size: 12px;" +
                            "}" +

                            "h1 {" +
                            "font-size: 26px;" +
                            "color: #594b3e;" +
                            "border-bottom: 1px solid #d8d0c5;" +
                            "padding-bottom: 12px;" +
                            "margin-bottom: 25px;" +
                            "}" +

                            ".note-content {" +
                            "font-size: 13px;" +
                            "line-height: 1.8;" +
                            "overflow-wrap: break-word;" +
                            "}" +

                            "p { margin-bottom: 12px; }" +
                            "ul, ol { padding-left: 25px; }" +
                            "li { margin-bottom: 5px; }" +
                            "strong, b { font-weight: bold; }" +
                            "em, i { font-style: italic; }" +
                            "u { text-decoration: underline; }" +

                            "</style></head><body></body></html>"
            );

            // Configure XHTML output
            pdfDocument.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.xhtml)
                    .charset(StandardCharsets.UTF_8)
                    .prettyPrint(true);

            Element htmlElement = pdfDocument.selectFirst("html");

            if (htmlElement != null) {
                htmlElement.attr("xmlns", "http://www.w3.org/1999/xhtml");
            }

            // Add note title
            pdfDocument.body()
                    .appendElement("h1")
                    .text(note.getTitle());

            // Add sanitized rich text content
            String safeContent = sanitizeHtml(note.getContent());

            Element contentElement = pdfDocument.body()
                    .appendElement("div")
                    .addClass("note-content");

            contentElement.html(safeContent);

            // Generate PDF
            try (ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                PdfRendererBuilder builder = new PdfRendererBuilder();

                builder.useFastMode();
                builder.withHtmlContent(pdfDocument.outerHtml(), null);
                builder.toStream(output);
                builder.run();

                byte[] pdfBytes = output.toByteArray();

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename("inkwell-note-" + id + ".pdf")
                                        .build()
                                        .toString()
                        )
                        .body(pdfBytes);
            }

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate PDF",
                    e
            );
        }
    }


    // READ ALL NOTES

    @GetMapping
    public String getAllNotes(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", defaultValue = "recent") String sortBy,
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            Model model) {

        // 1. Sorting configuration
        Sort sort;

        switch (sortBy) {
            case "oldest":
                sort = Sort.by(Sort.Direction.ASC, "updatedAt");
                break;

            case "titleAsc":
                sort = Sort.by(Sort.Direction.ASC, "title");
                break;

            case "titleDesc":
                sort = Sort.by(Sort.Direction.DESC, "title");
                break;

            default:
                sort = Sort.by(Sort.Direction.DESC, "updatedAt");
                sortBy = "recent";
        }

        // 2. Search notes
        List<Note> notes;

        if (keyword != null && !keyword.trim().isEmpty()) {
            notes = noteRepository.findByTitleContainingIgnoreCase(
                    keyword.trim(), sort);
        } else {
            notes = noteRepository.findAll(sort);
        }

        // 3. Apply filters
        if ("favourites".equals(filter)) {

            notes = notes.stream()
                    .filter(Note::isFavourite)
                    .toList();

        } else if ("pinned".equals(filter)) {

            notes = notes.stream()
                    .filter(Note::isPinned)
                    .toList();

        } else {
            filter = "all";
        }

        // 4. Generate plain-text previews
        Map<Long, String> notePreviews = new HashMap<>();

        for (Note note : notes) {

            String content = note.getContent() == null
                    ? ""
                    : note.getContent();

            String plainText = Jsoup.parse(content).text();

            notePreviews.put(note.getId(), plainText);
        }

        // 5. Send data to Thymeleaf
        model.addAttribute("notes", notes);
        model.addAttribute("notePreviews", notePreviews);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("filter", filter);

        return "notes/list";
    }



    // SHOW CREATE NOTE FORM
    @GetMapping("/new")
    public String showCreateForm(Model model) {

        model.addAttribute("note", new Note());
        model.addAttribute("folders", folderRepository.findAll());
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute("safeContent", "");

        return "notes/form";
    }

    // CREATE OR UPDATE NOTE
    @PostMapping("/save")
    public String saveNote(
            @ModelAttribute("note") Note formNote,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds) {

        Folder selectedFolder = null;

        if (folderId != null) {
            selectedFolder = folderRepository.findById(folderId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid folder ID"));
        }

        Set<Tag> selectedTags = new HashSet<>();

        if (tagIds != null) {
            selectedTags.addAll(tagRepository.findAllById(tagIds));
        }

        if (formNote.getId() == null) {

            formNote.setFolder(selectedFolder);
            formNote.setTags(selectedTags);

            noteRepository.save(formNote);

        } else {

            Note existingNote = noteRepository.findById(formNote.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid note ID"));

            // Save the previous content before updating
            if (!Objects.equals(
                    existingNote.getContent(),
                    formNote.getContent())) {

                NoteVersion version = new NoteVersion();

                version.setContent(existingNote.getContent());
                version.setSavedAt(LocalDateTime.now());
                version.setNote(existingNote);

                noteVersionRepository.save(version);
            }

// Update the note
            existingNote.setTitle(formNote.getTitle());
            existingNote.setContent(formNote.getContent());
            existingNote.setFolder(selectedFolder);
            existingNote.setTags(selectedTags);

            noteRepository.save(existingNote);
        }

        return "redirect:/notes";
    }

    // SHOW EDIT FORM
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid note ID"));

        model.addAttribute("note", note);
        model.addAttribute("folders", folderRepository.findAll());
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute("safeContent", sanitizeHtml(note.getContent()));

        return "notes/form";
    }

    // DELETE NOTE
    @PostMapping("/delete/{id}")
    public String deleteNote(@PathVariable Long id) {

        noteRepository.deleteById(id);

        return "redirect:/notes";
    }

    // READ SINGLE NOTE
    @GetMapping("/view/{id}")
    public String viewNote(@PathVariable Long id, Model model) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid note ID"));

        model.addAttribute("note", note);
        model.addAttribute("safeContent",
                sanitizeHtml(note.getContent()));

        return "notes/view";
    }

    @PostMapping("/favourite/{id}")
    public String toggleFavourite(@PathVariable Long id) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid note ID"));

        note.setFavourite(!note.isFavourite());

        noteRepository.save(note);

        return "redirect:/notes";
    }

    @PostMapping("/pin/{id}")
    public String togglePin(@PathVariable Long id) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid note ID"));

        note.setPinned(!note.isPinned());

        noteRepository.save(note);

        return "redirect:/notes";
    }

    private String sanitizeHtml(String content) {

        Safelist safelist = Safelist.none()
                .addTags("p", "br", "b", "strong", "i", "em",
                        "u", "ul", "ol", "li");

        return Jsoup.clean(content, safelist);
    }

    @GetMapping("/history/{id}")
    public String viewHistory(@PathVariable Long id, Model model) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid note ID"));

        List<NoteVersion> versions =
                noteVersionRepository.findVersionsByNoteId(id);

        // Convert previous HTML content into readable text previews
        Map<Long, String> versionPreviews = new HashMap<>();

        for (NoteVersion version : versions) {

            String content = version.getContent() == null
                    ? ""
                    : version.getContent();

            versionPreviews.put(
                    version.getId(),
                    Jsoup.parse(content).text()
            );
        }

        model.addAttribute("note", note);
        model.addAttribute("versions", versions);
        model.addAttribute("versionPreviews", versionPreviews);

        return "notes/history";
    }
}
