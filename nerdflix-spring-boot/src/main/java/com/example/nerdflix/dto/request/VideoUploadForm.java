package com.example.nerdflix.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class VideoUploadForm {//Checked

    @NotNull(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be 3–100 characters")
    private String title;

    @Size(max = 500, message = "Description is too long")
    private String description;

    @NotNull(message = "Category is required")
    private Integer categoryId;

    @NotNull(message = "Video file is required")
    private MultipartFile file;

    public VideoUploadForm() {
    }

    public VideoUploadForm(String title, String description, Integer categoryId, MultipartFile file) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
