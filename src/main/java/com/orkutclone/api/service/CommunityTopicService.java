package com.orkutclone.api.service;

import com.orkutclone.api.dto.community.*;
import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.CommunityTopic;
import com.orkutclone.api.model.TopicMessage;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.CommunityRepository;
import com.orkutclone.api.repository.CommunityTopicRepository;
import com.orkutclone.api.repository.TopicMessageRepository;
import com.orkutclone.api.support.RelativeTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityTopicService {

    /** Messages are paginated ten at a time, per the forum spec. */
    private static final int PAGE_SIZE = 10;

    private final CommunityRepository communityRepository;
    private final CommunityTopicRepository topicRepository;
    private final TopicMessageRepository messageRepository;

    @Transactional
    public TopicSummaryDTO createTopic(UUID communityId, CreateTopicRequest request) {
        User current = authenticatedUser();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
        requireForumEnabled(community);

        CommunityTopic topic = topicRepository.save(CommunityTopic.builder()
                .community(community)
                .title(request.title().trim())
                .author(current)
                .build());

        return new TopicSummaryDTO(topic.getId(), topic.getTitle(), community.getId(), community.getName(), 0L);
    }

    @Transactional
    public TopicMessageDTO postMessage(UUID topicId, CreateTopicMessageRequest request) {
        User current = authenticatedUser();
        CommunityTopic topic = topicRepository.findByIdWithCommunity(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        requireForumEnabled(topic.getCommunity());

        TopicMessage message = messageRepository.save(TopicMessage.builder()
                .topic(topic)
                .author(current)
                .subject(request.subject().trim())
                .message(request.message())
                .build());

        return toMessageDTO(message, current, Instant.now());
    }

    @Transactional(readOnly = true)
    public TopicMessagesPageDTO getMessages(UUID topicId, int page) {
        CommunityTopic topic = topicRepository.findByIdWithCommunity(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));

        Page<TopicMessage> pageResult = messageRepository.findPageByTopicId(
                topicId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));

        Instant now = Instant.now();
        List<TopicMessageDTO> messages = pageResult.getContent().stream()
                .map(message -> toMessageDTO(message, message.getAuthor(), now))
                .toList();

        return new TopicMessagesPageDTO(
                topic.getId(),
                topic.getTitle(),
                topic.getCommunity().getId(),
                topic.getCommunity().getName(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast(),
                pageResult.hasNext(),
                pageResult.hasPrevious(),
                messages);
    }

    /** The owner can switch the forum off in the community settings; writes are refused while it is. */
    private void requireForumEnabled(Community community) {
        if (!community.getFeatures().isForumEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The forum is disabled for this community");
        }
    }

    private TopicMessageDTO toMessageDTO(TopicMessage message, User author, Instant now) {
        return new TopicMessageDTO(
                message.getId(),
                author.getId(),
                author.getName(),
                author.getProfilePicture(),
                message.getSubject(),
                message.getMessage(),
                message.getCreatedAt() == null ? null : message.getCreatedAt().atOffset(ZoneOffset.UTC),
                RelativeTimeFormatter.format(message.getCreatedAt(), now));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
