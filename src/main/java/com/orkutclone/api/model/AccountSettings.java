package com.orkutclone.api.model;

import com.orkutclone.api.model.enums.ContentFilterMode;
import com.orkutclone.api.model.enums.FriendUpdatesScope;
import com.orkutclone.api.model.enums.SlowInternetMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "account_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private String language = "pt-BR";

    @Builder.Default
    @Column(name = "birthday_reminders", nullable = false)
    private boolean birthdayReminders = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "content_filter", nullable = false)
    private ContentFilterMode contentFilter = ContentFilterMode.HIDE_IMPROPER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "friend_updates_scope", nullable = false)
    private FriendUpdatesScope friendUpdatesScope = FriendUpdatesScope.ALL_FRIENDS;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "account_settings_profile_features", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "feature")
    private List<String> profileFeatures = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "slow_internet_mode", nullable = false)
    private SlowInternetMode slowInternetMode = SlowInternetMode.NORMAL;

    @Builder.Default
    @Column(name = "suppress_slow_internet_warning", nullable = false)
    private boolean suppressSlowInternetWarning = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
