package com.orkutclone.api.service;

import com.orkutclone.api.dto.community.CommunityDashboardDTO;
import com.orkutclone.api.dto.poll.*;
import com.orkutclone.api.exception.ConflictException;
import com.orkutclone.api.model.*;
import com.orkutclone.api.repository.*;
import com.orkutclone.api.repository.projection.PollOptionVoteCountProjection;
import com.orkutclone.api.support.AvatarStorageService;
import com.orkutclone.api.support.RelativeTimeFormatter;
import com.orkutclone.api.support.UploadedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommunityRepository communityRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollCommentRepository pollCommentRepository;
    private final AvatarStorageService imageStorage;

    /**
     * Stores an image and returns its public URL, to be sent back as {@code imageUrl} when
     * creating the poll. Uploading before the poll exists is what lets the form submit in a
     * single request.
     */
    public PollImageResponse uploadImage(MultipartFile file) {
        UploadedImage image = UploadedImage.from(file);
        return new PollImageResponse(imageStorage.store(image.data(), image.extension()));
    }

    @Transactional
    public PollDetailDTO create(UUID communityId, CreatePollRequest request) {
        User current = authenticatedUser();
        Community community = findCommunityOrThrow(communityId);
        requirePollsEnabled(community);

        Poll poll = pollRepository.save(Poll.builder()
                .community(community)
                .question(request.question().trim())
                .description(request.description() == null ? null : request.description().trim())
                .imageUrl(request.imageUrl())
                .creator(current)
                .closesAt(request.closesAt() == null ? null : request.closesAt().toInstant())
                .anonymous(Boolean.TRUE.equals(request.anonymous()))
                .multipleChoice(Boolean.TRUE.equals(request.multipleChoice()))
                .build());

        List<String> options = request.options();
        for (int i = 0; i < options.size(); i++) {
            pollOptionRepository.save(PollOption.builder()
                    .poll(poll)
                    .text(options.get(i).trim())
                    .orderIndex(i)
                    .build());
        }

        return toDetail(poll, current);
    }

    @Transactional(readOnly = true)
    public PollsPageDTO list(UUID communityId, int page, int size) {
        findCommunityOrThrow(communityId);
        User viewer = authenticatedUser();

        PageRequest pageRequest = pageRequest(page, size);
        Page<Poll> pollsPage = pollRepository.findPageByCommunityId(communityId, pageRequest);

        List<PollSummaryDTO> results = pollsPage.getContent().stream()
                .map(poll -> toSummary(poll, communityId, viewer))
                .toList();

        return new PollsPageDTO(page, size, pollsPage.getTotalElements(), pollsPage.getTotalPages(), results);
    }

    @Transactional(readOnly = true)
    public PollDetailDTO getDetail(UUID communityId, UUID pollId) {
        User viewer = authenticatedUser();
        Poll poll = findPollOrThrow(communityId, pollId);
        return toDetail(poll, viewer);
    }

    @Transactional
    public void delete(UUID communityId, UUID pollId) {
        Poll poll = findPollOrThrow(communityId, pollId);
        requireOwner(poll.getCommunity());

        pollVoteRepository.deleteByPollId(pollId);
        pollCommentRepository.deleteByPollId(pollId);
        pollOptionRepository.deleteByPollId(pollId);
        pollRepository.delete(poll);
    }

    @Transactional
    public PollDetailDTO vote(UUID communityId, UUID pollId, VoteRequest request) {
        User current = authenticatedUser();
        Poll poll = findPollOrThrow(communityId, pollId);
        requirePollsEnabled(poll.getCommunity());
        requireOpen(poll);

        List<UUID> optionIds = request.optionIds().stream().distinct().toList();
        if (!poll.isMultipleChoice() && optionIds.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This poll only allows a single choice");
        }

        if (pollVoteRepository.existsByPollIdAndVoterId(pollId, current.getId())) {
            throw new ConflictException("You already voted in this poll");
        }

        for (UUID optionId : optionIds) {
            PollOption option = pollOptionRepository.findByIdAndPollId(optionId, pollId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll option not found"));
            pollVoteRepository.save(PollVote.builder()
                    .poll(poll)
                    .option(option)
                    .voter(current)
                    .build());
        }

        return toDetail(poll, current);
    }

    @Transactional
    public PollCommentDTO addComment(UUID communityId, UUID pollId, CreatePollCommentRequest request) {
        User current = authenticatedUser();
        Poll poll = findPollOrThrow(communityId, pollId);
        requirePollsEnabled(poll.getCommunity());

        PollComment comment = pollCommentRepository.save(PollComment.builder()
                .poll(poll)
                .author(current)
                .message(request.message())
                .build());

        return toCommentDTO(comment, poll);
    }

    /** Consumed by {@link CommunityDashboardService} to populate the home page's poll widget. */
    @Transactional(readOnly = true)
    public CommunityDashboardDTO.ActivePollDTO getActivePollForDashboard(UUID communityId) {
        return pollRepository.findFirstByCommunityIdOrderByCreatedAtDesc(communityId)
                .map(poll -> {
                    Map<UUID, Long> voteCounts = voteCountsByOption(poll.getId());

                    List<CommunityDashboardDTO.PollOptionDTO> voteOptions = pollOptionRepository
                            .findByPollIdOrderByOrderIndexAsc(poll.getId()).stream()
                            .map(o -> new CommunityDashboardDTO.PollOptionDTO(
                                    o.getId(), o.getText(), voteCounts.getOrDefault(o.getId(), 0L)))
                            .toList();

                    return new CommunityDashboardDTO.ActivePollDTO(
                            poll.getId(),
                            poll.getQuestion(),
                            poll.getImageUrl(),
                            poll.getCreator().getName(),
                            toOffsetDateTime(poll.getClosesAt()),
                            isClosed(poll),
                            poll.isMultipleChoice(),
                            voteOptions);
                })
                .orElse(null);
    }

    /** The owner can switch polls off in the community settings; writes are refused while it is. */
    private void requirePollsEnabled(Community community) {
        if (!community.getFeatures().isPollsEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Polls are disabled for this community");
        }
    }

    /** A poll with a past {@code closesAt} no longer accepts votes. */
    private void requireOpen(Poll poll) {
        if (isClosed(poll)) {
            throw new ConflictException("This poll is closed");
        }
    }

    private boolean isClosed(Poll poll) {
        return poll.getClosesAt() != null && Instant.now().isAfter(poll.getClosesAt());
    }

    private void requireOwner(Community community) {
        User current = authenticatedUser();
        if (community.getOwner() == null || !community.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the community owner can do this");
        }
    }

    private Community findCommunityOrThrow(UUID communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }

    private Poll findPollOrThrow(UUID communityId, UUID pollId) {
        return pollRepository.findByIdAndCommunityId(pollId, communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));
    }

    private Map<UUID, Long> voteCountsByOption(UUID pollId) {
        return pollVoteRepository.countByOptionForPoll(pollId).stream()
                .collect(Collectors.toMap(PollOptionVoteCountProjection::getOptionId, PollOptionVoteCountProjection::getVoteCount));
    }

    private PollSummaryDTO toSummary(Poll poll, UUID communityId, User viewer) {
        long totalVotes = pollVoteRepository.countByPollId(poll.getId());
        long commentsCount = pollCommentRepository.countByPollId(poll.getId());
        boolean viewerVoted = pollVoteRepository.existsByPollIdAndVoterId(poll.getId(), viewer.getId());

        return new PollSummaryDTO(
                poll.getId(),
                communityId,
                poll.getQuestion(),
                poll.getImageUrl(),
                poll.getCreator().getId(),
                poll.getCreator().getName(),
                toOffsetDateTime(poll.getCreatedAt()),
                toOffsetDateTime(poll.getClosesAt()),
                isClosed(poll),
                poll.isAnonymous(),
                poll.isMultipleChoice(),
                totalVotes,
                commentsCount,
                viewerVoted);
    }

    private PollDetailDTO toDetail(Poll poll, User viewer) {
        Map<UUID, Long> voteCounts = voteCountsByOption(poll.getId());

        List<PollOptionDTO> optionDTOs = pollOptionRepository.findByPollIdOrderByOrderIndexAsc(poll.getId()).stream()
                .map(o -> new PollOptionDTO(o.getId(), o.getText(), voteCounts.getOrDefault(o.getId(), 0L)))
                .toList();

        long totalVotes = optionDTOs.stream().mapToLong(PollOptionDTO::voteCount).sum();

        List<UUID> viewerVoteOptionIds = pollVoteRepository.findAllByPollIdAndVoterId(poll.getId(), viewer.getId()).stream()
                .map(v -> v.getOption().getId())
                .toList();

        List<PollCommentDTO> comments = pollCommentRepository.findByPollIdOrderByCreatedAtAsc(poll.getId()).stream()
                .map(comment -> toCommentDTO(comment, poll))
                .toList();

        return new PollDetailDTO(
                poll.getId(),
                poll.getCommunity().getId(),
                poll.getCommunity().getName(),
                poll.getQuestion(),
                poll.getDescription(),
                poll.getImageUrl(),
                poll.getCreator().getId(),
                poll.getCreator().getName(),
                toOffsetDateTime(poll.getCreatedAt()),
                toOffsetDateTime(poll.getClosesAt()),
                isClosed(poll),
                poll.isAnonymous(),
                poll.isMultipleChoice(),
                optionDTOs,
                totalVotes,
                viewerVoteOptionIds,
                comments);
    }

    /** The voted-option tag is omitted for anonymous polls, and for authors who haven't voted. */
    private PollCommentDTO toCommentDTO(PollComment comment, Poll poll) {
        List<PollVote> votes = poll.isAnonymous()
                ? List.of()
                : pollVoteRepository.findAllByPollIdAndVoterId(poll.getId(), comment.getAuthor().getId());

        return new PollCommentDTO(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getAuthor().getProfilePicture(),
                comment.getMessage(),
                toOffsetDateTime(comment.getCreatedAt()),
                RelativeTimeFormatter.format(comment.getCreatedAt(), Instant.now()),
                votes.stream().map(v -> v.getOption().getId()).toList(),
                votes.stream().map(v -> v.getOption().getText()).toList());
    }

    private static java.time.OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
