package com.orkutclone.api.model;

import com.orkutclone.api.model.enums.PrivacyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile_social")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    private String children;
    private String ethnicity;
    private String religion;
    private String politicalView;
    private String sexualOrientation;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel sexualOrientationPrivacy;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "user_profile_humor", joinColumns = @JoinColumn(name = "social_id"))
    @Column(name = "humor")
    private List<String> humor = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "user_profile_style", joinColumns = @JoinColumn(name = "social_id"))
    @Column(name = "style")
    private List<String> style = new ArrayList<>();

    private String smoking;
    private String drinking;
    private String pets;
    private String livingWith;
    private String hometown;
    private String website;

    @Column(columnDefinition = "text")
    private String aboutMe;

    @Column(columnDefinition = "text")
    private String passions;

    @Column(columnDefinition = "text")
    private String sports;

    @Column(columnDefinition = "text")
    private String activities;

    @Column(columnDefinition = "text")
    private String books;

    @Column(columnDefinition = "text")
    private String music;

    @Column(columnDefinition = "text")
    private String tvShows;

    @Column(columnDefinition = "text")
    private String movies;

    @Column(columnDefinition = "text")
    private String cuisines;
}
