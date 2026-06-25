package com.orkutclone.api.model;

import com.orkutclone.api.model.enums.PrivacyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile_contact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    private String primaryEmail;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel primaryEmailPrivacy;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "user_profile_secondary_emails", joinColumns = @JoinColumn(name = "contact_id"))
    private List<SecondaryEmail> secondaryEmails = new ArrayList<>();

    private String im1;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel im1Privacy;

    private String im2;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel im2Privacy;

    private String homePhone;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel homePhonePrivacy;

    private String mobilePhone;

    @Enumerated(EnumType.STRING)
    private PrivacyLevel mobilePhonePrivacy;

    private String address1;
    private String address2;
    private String addressCity;
    private String addressState;
    private String addressZipCode;
    private String addressCountry;
}
