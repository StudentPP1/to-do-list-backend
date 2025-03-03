package com.example.backend.tag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public String addTag(String userId, String name, String color) {
        Tag tag = new Tag(name, color, userId);
        tagRepository.save(tag);
        return tag.getId();
    }

    public void updateTag(String tagId, String name, String color) throws NoSuchElementException{
        Tag tag = getTag(tagId);
        tag.setName(name);
        tag.setColor(color);
        tagRepository.save(tag);
    }

    public void deleteTag(String tagId) {
        tagRepository.deleteById(tagId);
    }
    public void deleteTags(List<String> tagsId) {
        tagRepository.deleteAllById(tagsId);
    }

    public Tag getTag(String id) throws NoSuchElementException {
        Optional<Tag> optionalTag = tagRepository.findById(id);
        optionalTag.orElseThrow(() -> new NoSuchElementException("Tag with id " + id + " not found"));
        return optionalTag.get();
    }

    public List<Tag> getAllTags(List<String> tagsId) {
        System.out.println(tagsId);
        List<Tag> taskTags =  new ArrayList<>();
        for (String tagId: tagsId) {
            Optional<Tag> optionalTag = tagRepository.findById(tagId);
            optionalTag.orElseThrow(() -> new NoSuchElementException("Tag with id " + tagId + " not found"));
            taskTags.add(optionalTag.get());
        }
        return taskTags;
    }
}
