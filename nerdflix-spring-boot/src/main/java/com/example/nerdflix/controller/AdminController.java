package com.example.nerdflix.controller;

import com.example.nerdflix.dto.request.VideoUpdateForm;
import com.example.nerdflix.dto.request.VideoUploadForm;
import com.example.nerdflix.entity.Category;
import com.example.nerdflix.entity.Video;
import com.example.nerdflix.service.CategoryService;
import com.example.nerdflix.service.VideoProcessingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
//@RequestMapping("/admin")
public class AdminController {//Checked

    private final VideoProcessingService videoService;
    private final CategoryService categoryService;

    

    private static final List<String> ALLOWED_TYPES = List.of(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-matroska",
            "video/x-msvideo",
            "video/mpeg",
            "video/3gpp",
            "video/3gpp2",
            "video/ogg",
            "video/x-ms-wmv",
            "video/x-flv",
            "video/x-m4v",
            "video/matroska"
    );

    @Autowired
    public AdminController(VideoProcessingService videoService, CategoryService categoryService) {
        this.videoService = videoService;
        this.categoryService = categoryService;
    }

    @GetMapping("/admin/videos")
    public String listVideos(Model model) {
        model.addAttribute("videos", videoService.findAll());
        model.addAttribute("categories", categoryService.getCategories());
        return "admin/videos-list";
    }

    @GetMapping("/admin/upload")
    public String showUploadForm(Model model) {

        if (!model.containsAttribute("videoForm")) {
            model.addAttribute("videoForm", new VideoUploadForm());
        }

        model.addAttribute("categories", categoryService.getCategories());

        return "admin/upload-video";
    }

    @PostMapping("/admin/upload")
    public String handleUpload(
            @Valid @ModelAttribute("videoForm") VideoUploadForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) throws Exception {

        model.addAttribute("categories", categoryService.getCategories());

        if (form.getFile()==null || form.getFile().isEmpty()) {
            bindingResult.rejectValue(
                    "file",
                    "file.empty",
                    "Please select a video file."
            );
        } else if (!ALLOWED_TYPES.contains(form.getFile().getContentType())) {
          //  System.out.println("video type: " +  form.getFile().getContentType());
            bindingResult.rejectValue(
                    "file",
                    "file.type",
                    "Unsupported video format. Allowed: MP4, WebM, MOV, MKV."
            );
        }

        if (bindingResult.hasErrors()) {
            return "admin/upload-video";
        }

        Category category = categoryService.getCategoryById(form.getCategoryId());

        Video video = new Video();
        video.setTitle(form.getTitle());
        video.setDescription(form.getDescription());
        video.setCategory(category);

        try {
            UUID videoId = videoService.processAndSave(form.getFile(), video).join();

            redirectAttributes.addFlashAttribute("success",
                    "Video uploaded successfully!");

            return "redirect:/admin/upload";

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Upload failed, Please Try Again");
            redirectAttributes.addFlashAttribute("videoForm", form);

            System.out.println(ex.getMessage());

            return "redirect:/admin/upload";
        }
    }

    @GetMapping("/admin/videos/{id}")
    public String editVideo(@PathVariable UUID id, Model model) {
        Video video = videoService.getVideoById(id);

        VideoUpdateForm form = new VideoUpdateForm();
        form.setId(video.getId());
        form.setTitle(video.getTitle());
        form.setDescription(video.getDescription());
        form.setCategoryId(video.getCategory().getId());

        model.addAttribute("videoForm", form);
        model.addAttribute("categories", categoryService.getCategories());

        return "admin/edit-video";
    }

    @PostMapping("/admin/videos/update")
    public String updateVideo(@ModelAttribute VideoUpdateForm form, RedirectAttributes redirectAttributes) {

        Category category = categoryService.getCategoryById(form.getCategoryId());

        videoService.updateVideo(form, category);
        redirectAttributes.addFlashAttribute("success", "Video updated!");

        return "redirect:/admin/videos";
    }

    @PostMapping("/admin/videos/delete/{id}")
    public String deleteVideo(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            videoService.deleteVideo(id);
            redirectAttributes.addFlashAttribute("success", "Video deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed!");
        }
        return "redirect:/admin/videos";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
