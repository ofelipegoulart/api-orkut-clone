package com.orkutclone.api.model;

import com.orkutclone.api.model.enums.PrivacyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile_general")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    private String firstName;
    private String lastName;
    private String gender;
    private String relationshipStatus;
    private String birthMonth;
    private String birthDay;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel birthDatePrivacy;

    private String birthYear;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel birthYearPrivacy;

    private String city;
    private String state;
    private String zipCode;
    private String country;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "user_profile_languages", joinColumns = @JoinColumn(name = "general_id"))
    @Column(name = "language")
    private List<String> languages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private PrivacyLevel languagesPrivacy;

    private String highSchool;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel highSchoolPrivacy;

    private String college;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel collegePrivacy;

    private String company;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel companyPrivacy;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "user_profile_interested_in", joinColumns = @JoinColumn(name = "general_id"))
    @Column(name = "interest")
    private List<String> interestedIn = new ArrayList<>();

    private String datingPreference;
}
