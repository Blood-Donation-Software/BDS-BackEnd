package com.blooddonation.blood_donation_support_system.controller;

import com.blooddonation.blood_donation_support_system.dto.AccountDto;
import com.blooddonation.blood_donation_support_system.dto.BlogDto;
import com.blooddonation.blood_donation_support_system.service.BlogRequestService;
import com.blooddonation.blood_donation_support_system.service.BlogService;
import com.blooddonation.blood_donation_support_system.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;

@Slf4j
@RestController
@RequestMapping("/api/blog")
public class BlogController {
    @Autowired
    private BlogService blogService;
    @Autowired
    private BlogRequestService blogRequestService;
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/list-blogs")
    public ResponseEntity<Page<BlogDto>> getBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        try {
            Page<BlogDto> blogs = blogService.getSortedPaginatedBlogs(page, size, sortBy, ascending);
            return ResponseEntity.ok(blogs);
        } catch (Exception e) {
            log.error("Failed to get blogs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/list-blogs/{blogId}")
    public ResponseEntity<Object> getBlogDetails(@PathVariable Long blogId) {
        try {
            BlogDto blogDto = blogService.getBlogDetails(blogId);
            return ResponseEntity.ok(blogDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/my-blogs")
    public ResponseEntity<Page<BlogDto>> getMyBlogs(
            @CookieValue(value = "jwt-token", required = false) String token,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AccountDto staff = jwtUtil.extractUser(token);
            Page<BlogDto> blogs = blogService.getMyBlogs(staff.getEmail(), pageNumber, pageSize, sortBy, ascending);
            return ResponseEntity.ok(blogs);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/my-blogs/{blogId}")
    public ResponseEntity<BlogDto> getMyBlogDetails(@PathVariable Long blogId,
                                                    @CookieValue(value = "jwt-token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        try {
            // controller delegates authorization/ownership checks to service/validator
            BlogDto blogDto = blogService.getMyBlogDetails(blogId);
            return ResponseEntity.ok(blogDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping(value = "my-blogs/{blogId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateBlog(
            @PathVariable Long blogId,
            @Valid @RequestPart("blog") BlogDto blogDto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @CookieValue(value = "jwt-token", required = false) String token) {

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing authentication token");
        }

        try {
            AccountDto account = jwtUtil.extractUser(token);
            String result = blogRequestService.updateBlogRequest(account.getId(),blogId, blogDto, thumbnail);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update blog");
        }
    }

    @PostMapping("/my-blogs/{blogId}/delete")
    public ResponseEntity<String> deleteBlog(
            @PathVariable Long blogId,
            @CookieValue(value = "jwt-token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing authentication token");
        }
        try {
            AccountDto account = jwtUtil.extractUser(token);
            String result = blogRequestService.deleteBlogRequest(account.getId(), blogId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update blog");
        }
    }

}
