package org.project.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.project.core.model.Image;
import org.project.core.model.Mail;
import org.project.core.model.User;
import org.project.core.repository.ImageRepository;
import org.project.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${yandex.cloud.token}")
    private String token;

    @Value("${yandex.cloud.uploadUrl}")
    private String uploadUrl;

    @Value("${yandex.cloud.downloadUrl}")
    private String downloadUrl;

    @Value("${app.file.storage.path}")
    private String fileStoragePath;

    @Value("${app.file.storage.type}")
    private String storageType;

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final QueueProducer queueProducer;

    public User getCurrentUser() {
        log.info("Getting current user");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new UsernameNotFoundException("User not found");
        }

        log.info("Authentication object: " + authentication);
        log.info("Authentication isAuthenticated: " + authentication.isAuthenticated());

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public void uploadFiles(MultipartFile files, Long uploaderId) throws IOException, ChangeSetPersister.NotFoundException {
        if ("cloud".equalsIgnoreCase(storageType)) {
            uploadFilesToCloud(files, uploaderId);
        } else if ("disk".equalsIgnoreCase(storageType)) {
            uploadFilesToDisk(files, uploaderId);
        } else {
            throw new IllegalArgumentException("Invalid storage type specified");
        }
    }

    public void uploadFilesToCloud(MultipartFile file, Long uploaderId) throws IOException, ChangeSetPersister.NotFoundException {
            validateFile(file);
            File tempFile = convertToFile(file);
            try {
                uploadFileToCloud(tempFile, uploaderId);
            } finally {
                tempFile.delete();
            }
    }

    public void uploadFilesToDisk(MultipartFile file, Long uploaderId) throws IOException, ChangeSetPersister.NotFoundException {
            var user = userRepository.findById(uploaderId);
            if (user.isPresent()) {
                validateFile(file);
                File savedFile = saveFileToDisk(file);
                createAndSaveImage(savedFile, user.get(), "local");
            }
    }

    private void validateFile(MultipartFile file) {
        if (!file.getContentType().equals("image/jpeg") && !file.getContentType().equals("image/png")) {
            throw new IllegalArgumentException("Only JPEG and PNG files are allowed");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 10 MB");
        }
    }

    private File convertToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private void uploadFileToCloud(File file, Long uploaderId) throws IOException, ChangeSetPersister.NotFoundException {
        String encodedFileName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            HttpPost httpPost = new HttpPost(uploadUrl + "?path=" + encodedFileName);
            httpPost.setHeader("Authorization", "OAuth " + token);
            HttpResponse response = client.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonResponse);
            if (!jsonNode.has("href")) {
                throw new IOException("Upload URL not found in response.");
            }
            String href = jsonNode.get("href").asText();

            HttpPut httpPut = new HttpPut(href);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file);
            httpPut.setEntity(builder.build());

            client.execute(httpPut);

            Optional<User> user = userRepository.findById(uploaderId);
            if (user.isPresent()) {
                Image photo = createAndSaveImage(file, user.get(), href);
                sendMail(user.get(), photo);
            } else {
                throw new ChangeSetPersister.NotFoundException();
            }
        } finally {
            client.close();
        }
    }

    private File saveFileToDisk(MultipartFile file) throws IOException {
        Path path = Paths.get(fileStoragePath, file.getOriginalFilename());
        Files.write(path, file.getBytes());
        return path.toFile();
    }


    public byte[] downloadFile(Long id) throws IOException, ChangeSetPersister.NotFoundException {
        if ("cloud".equalsIgnoreCase(storageType)) {
            return downloadFileFromCloud(id);
        } else if ("disk".equalsIgnoreCase(storageType)) {
            return downloadFileFromDisk(id);
        } else {
            throw new IllegalArgumentException("Invalid storage type specified");
        }
    }

    public byte[] downloadFileFromCloud(Long id) throws IOException, ChangeSetPersister.NotFoundException {
        var file = imageRepository.findById(id);
        if (file.isPresent()) {
            var fileName = file.get().getName();
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(downloadUrl + "?path=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            httpGet.setHeader("Authorization", "OAuth " + token);

            HttpResponse response = client.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            client.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonResponse);
            String href = jsonNode.get("href").asText();

            httpGet = new HttpGet(href);
            response = client.execute(httpGet);
            byte[] fileContent = EntityUtils.toByteArray(response.getEntity());
            client.close();
            return fileContent;
        } else {
            throw new ChangeSetPersister.NotFoundException();
        }
    }

    public byte[] downloadFileFromDisk(Long id) throws IOException, ChangeSetPersister.NotFoundException {
        var file = imageRepository.findById(id);
        if (file.isPresent()) {
            Path path = Paths.get(file.get().getUrl());
            return Files.readAllBytes(path);
        } else {
            throw new ChangeSetPersister.NotFoundException();
        }
    }

    public List<Image> getAllImages(Long id, Map<String, String> params) {
        String sortBy = params.getOrDefault("sortBy", "uploadDate");
        String sortDir = params.getOrDefault("sortDir", "asc");

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        return imageRepository.findAllByUserId(id, sort);
    }
    private Image createAndSaveImage(File file, User user, String href) {
        Image photo = new Image();
        photo.setName(file.getName());
        photo.setUser(user);
        photo.setUrl(href);
        photo.setSize(file.length());
        imageRepository.save(photo);
        return photo;
    }

    private void sendMail(User user, Image photo) {
        Mail mail = new Mail();
        mail.setToAddress(user.getEmail());
        mail.setMessage("Image " + photo.getName() + " was uploaded, size= " + photo.getSize());
        queueProducer.sendMessage(mail);
    }
}
