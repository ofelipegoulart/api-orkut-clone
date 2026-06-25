package com.orkutclone.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile_personal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfilePersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    private String eyeColor;
    private String hairColor;
    private String height;
    private String bodyType;
    private String appearance;
    private String bodyArt;

    @Column(columnDefinition = "text")
    private String perfectMatch;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "user_profile_attractions", joinColumns = @JoinColumn(name = "personal_id"))
    @Column(name = "attraction")
    private List<String> attractions = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String cantStand;

    @Column(columnDefinition = "text")
    private String idealFirstDate;

    @Column(columnDefinition = "text")
    private String pastRelationshipsLessons;

    @Column(columnDefinition = "text")
    private String whatStandsOut;

    private String favoriteBodyPart;

    @Column(columnDefinition = "text")
    private String fiveEssentials;

    @Column(columnDefinition = "text")
    private String inMyRoom;
}
