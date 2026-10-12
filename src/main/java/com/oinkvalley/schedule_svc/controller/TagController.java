package com.oinkvalley.schedule_svc.controller;

import com.oinkvalley.schedule_svc.dto.tag.AddTagMemberRequest;
import com.oinkvalley.schedule_svc.dto.tag.CreateTagRequest;
import com.oinkvalley.schedule_svc.dto.tag.DiscoverTagResponse;
import com.oinkvalley.schedule_svc.dto.tag.SetHiddenTagsRequest;
import com.oinkvalley.schedule_svc.dto.tag.TagListResponse;
import com.oinkvalley.schedule_svc.dto.tag.TagResponse;
import com.oinkvalley.schedule_svc.dto.tag.UpdateTagNameRequest;
import com.oinkvalley.schedule_svc.dto.tag.UpdateTagVisibilityRequest;
import com.oinkvalley.schedule_svc.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<TagListResponse> list(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.listForUser(userId));
    }

    @GetMapping("/discover")
    public ResponseEntity<List<DiscoverTagResponse>> discover(
            @AuthenticationPrincipal Long userId,
            @RequestParam String email
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.discoverByOwnerEmail(email));
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTagRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.create(userId, request));
    }

    @PutMapping("/hidden")
    public ResponseEntity<Void> replaceHidden(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SetHiddenTagsRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        tagService.setHiddenTags(userId, request.tagIds());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> delete(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        tagService.deleteOwnedTag(userId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<TagResponse> rename(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateTagNameRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.rename(userId, tagId, request.name()));
    }

    @PutMapping("/{tagId}/visibility")
    public ResponseEntity<TagResponse> updateVisibility(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateTagVisibilityRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.updateVisibility(userId, tagId, request.visibility()));
    }

    @PutMapping("/{tagId}/hidden")
    public ResponseEntity<Void> setTagHidden(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId,
            @RequestParam boolean hidden
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        tagService.setTagHidden(userId, tagId, hidden);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tagId}/follow")
    public ResponseEntity<TagResponse> follow(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.followTag(userId, tagId));
    }

    @DeleteMapping("/{tagId}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        tagService.unfollowTag(userId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tagId}/members")
    public ResponseEntity<TagResponse> addMember(
            @PathVariable long tagId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AddTagMemberRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(tagService.addMember(userId, tagId, request.email()));
    }

    @DeleteMapping("/{tagId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable long tagId,
            @PathVariable long memberUserId,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        tagService.removeMember(userId, tagId, memberUserId);
        return ResponseEntity.noContent().build();
    }
}
