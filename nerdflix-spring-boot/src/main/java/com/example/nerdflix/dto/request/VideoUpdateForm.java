package com.example.nerdflix.dto.request;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VideoUpdateForm {

    private UUID id;

    @NotNull(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be 3–100 characters")
    private String title;

    @Size(max = 500, message = "Description is too long")
    private String description;

    @NotNull(message = "Category is required")
    private Integer categoryId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
