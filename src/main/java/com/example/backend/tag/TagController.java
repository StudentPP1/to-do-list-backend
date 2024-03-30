package com.example.backend.tag;

import com.example.backend.task.RequestTask;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/get")
    public Tag getTag(@RequestParam(name = "tagId") String tagId) {
        return tagService.getTag(tagId);
    }

    @PostMapping("/getAll")
    public List<Tag> getAllTags(@RequestBody(required = false) RequestTags tagsId) {
        System.out.println(tagsId);
        return tagService.getAllTags(tagsId.getTags());
    }

    @PostMapping("/update")
    public void updateTag(@RequestBody Map<String, String> request) {
        tagService.updateTag(request.get("tagId"), request.get("name"), request.get("color"));
    }
}
