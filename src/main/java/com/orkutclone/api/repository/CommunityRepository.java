package com.orkutclone.api.repository;

import com.orkutclone.api.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityRepository extends JpaRepository<Community, UUID> {
}