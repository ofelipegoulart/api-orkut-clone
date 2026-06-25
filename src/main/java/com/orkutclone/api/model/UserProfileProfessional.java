package com.orkutclone.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_profile_professional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileProfessional {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    private String education;
    private String school;
    private String college;
    private String course;
    private String degree;

    @Column(name = "graduation_year")
    private String graduationYear;

    private String profession;
    private String sector;
    private String company;

    @Column(columnDefinition = "text")
    private String jobDescription;

    private String workPhone;

    @Column(columnDefinition = "text")
    private String professionalSkills;

    @Column(columnDefinition = "text")
    private String professionalInterests;
}
