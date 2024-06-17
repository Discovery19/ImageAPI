package org.project.core.service;

import lombok.RequiredArgsConstructor;
import org.project.core.model.Image;
import org.project.core.model.User;
import org.project.core.repository.ImageRepository;
import org.project.core.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    public List<Image> getAllImages(Map<String, String> params) {
        String sortBy = params.getOrDefault("sortBy", "uploadDate");
        String sortDir = params.getOrDefault("sortDir", "asc");

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        return imageRepository.findAll(sort);
    }
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(false);
        userRepository.saveAndFlush(user);
    }

    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.saveAndFlush(user);
    }
}
