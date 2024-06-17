package org.project.core.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.core.model.Image;
import org.project.core.model.User;
import org.project.core.service.UserService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/user")
public class UserController {
    private final UserService userService;


    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(@RequestParam("file") MultipartFile file) throws IOException, ChangeSetPersister.NotFoundException {
        userService.uploadFiles(file, userService.getCurrentUser().getId());
        return ResponseEntity.ok().body("File uploaded");
    }

    @GetMapping("/images")
    public ResponseEntity<List<Image>> getUserImages(@RequestParam Map<String, String> params) {
        User currentUser = userService.getCurrentUser();
        List<Image> images = userService.getAllImages(currentUser.getId(), params);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable Long imageId) throws IOException, ChangeSetPersister.NotFoundException {
        byte[] fileContent = userService.downloadFile(imageId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(fileContent);
    }

}
