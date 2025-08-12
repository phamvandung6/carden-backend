package com.loopy.carden.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topic extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @Column(name = "is_system_topic")
    private boolean isSystemTopic = false;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Self-referencing for hierarchical topics
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_topic_id")
    private Topic parentTopic;

    @OneToMany(mappedBy = "parentTopic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Topic> childTopics;

    // Relationships
    @OneToMany(mappedBy = "topic", fetch = FetchType.LAZY)
    private Set<Deck> decks;
}

