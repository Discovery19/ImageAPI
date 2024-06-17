package org.project.core.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String name;
    private Long size;
    private String url;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime uploadDate = LocalDateTime.now();
}
