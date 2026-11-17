
package com.inkwell.controller;

import com.inkwell.model.Folder;
import com.inkwell.repository.FolderRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/folders")
public class FolderController {

    private final FolderRepository folderRepository;

    public FolderController(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }


    @GetMapping("/{id}")
    public String viewFolder(@PathVariable Long id, Model model) {

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid folder ID"));

        model.addAttribute("folder", folder);

        return "folders/view";
    }
    
    // DISPLAY ALL FOLDERS
    @GetMapping
    public String getAllFolders(
            @RequestParam(required = false) String error,
            Model model) {

        model.addAttribute("folders", folderRepository.findAll());
        model.addAttribute("error", error);

        return "folders/list";
    }

    // SHOW CREATE FORM
    @GetMapping("/new")
    public String showCreateForm(Model model) {

        model.addAttribute("folder", new Folder());

        return "folders/form";
    }

    // SAVE NEW FOLDER OR UPDATE EXISTING FOLDER
    @PostMapping("/save")
    public String saveFolder(
            @ModelAttribute("folder") Folder formFolder) {

        if (formFolder.getId() == null) {

            folderRepository.save(formFolder);

        } else {

            Folder existingFolder = folderRepository
                    .findById(formFolder.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid folder ID")
                    );

            existingFolder.setName(formFolder.getName());

            folderRepository.save(existingFolder);
        }

        return "redirect:/folders";
    }

    // SHOW EDIT FORM
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid folder ID")
                );

        model.addAttribute("folder", folder);

        return "folders/form";
    }

    // DELETE EMPTY FOLDER
    @PostMapping("/delete/{id}")
    public String deleteFolder(@PathVariable Long id) {

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid folder ID")
                );

        if (!folder.getNotes().isEmpty()) {
            return "redirect:/folders?error=not-empty";
        }

        folderRepository.delete(folder);

        return "redirect:/folders";
    }
}
