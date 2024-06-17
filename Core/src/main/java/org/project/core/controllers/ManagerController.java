package org.project.core.controllers;

import lombok.RequiredArgsConstructor;
import org.project.core.model.Image;
import org.project.core.service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
public class ManagerController {
    private final ManagerService managerService;
//    Для получения списка изображений с сортировкой и фильтрацией:
//
//    GET /manager/images?sortBy=uploadDate&sortDir=desc
//    GET /manager/images?sortBy=size&sortDir=asc
//    Для блокировки пользователя:
//
//    POST /manager/block/{userId}
//    Для разблокировки пользователя:
//
//    POST /manager/unblock/{userId}

    @GetMapping("/images")
    public ResponseEntity<List<Image>> getAllImages(@RequestParam Map<String, String> params) {
        List<Image> images = managerService.getAllImages(params);
        return ResponseEntity.ok(images);
    }

    @PostMapping("/block/{userId}")
    public ResponseEntity<?> blockUser(@PathVariable Long userId) {
        managerService.blockUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable Long userId) {
        managerService.unblockUser(userId);
        return ResponseEntity.ok().build();
    }
}
