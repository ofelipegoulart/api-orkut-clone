package com.orkutclone.api.repository;

import com.orkutclone.api.model.ProfileFriend;
import com.orkutclone.api.repository.projection.FriendOverviewProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProfileFriendRepository extends JpaRepository<ProfileFriend, UUID> {

    @Query("SELECT COUNT(f) FROM ProfileFriend f WHERE f.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);

    void deleteByUserIdAndFriendIdOrUserIdAndFriendId(UUID userId1, UUID friendId1, UUID userId2, UUID friendId2);

        @Query("""
                        SELECT COUNT(DISTINCT viewerFriend.friend.id)
                        FROM ProfileFriend viewerFriend
                        JOIN ProfileFriend friendFriend ON friendFriend.friend.id = viewerFriend.friend.id
                        WHERE viewerFriend.user.id = :viewerId
                            AND friendFriend.user.id = :friendId
                        """)
        long countMutualFriends(@Param("viewerId") UUID viewerId, @Param("friendId") UUID friendId);

    List<ProfileFriend> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
            SELECT f.friend.id as id,
                   f.friend.name as name,
                   f.friend.profilePicture as avatar,
                   (SELECT COUNT(ff) FROM ProfileFriend ff WHERE ff.user.id = f.friend.id) as friendsCount
            FROM ProfileFriend f
            WHERE f.user.id = :userId
            ORDER BY f.createdAt DESC
            """)
    List<FriendOverviewProjection> findOverviewByUserId(@Param("userId") UUID userId, Pageable pageable);
}