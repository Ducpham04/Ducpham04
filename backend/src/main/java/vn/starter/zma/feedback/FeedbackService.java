package vn.starter.zma.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;
import vn.starter.zma.common.util.PageRequests;
import vn.starter.zma.feedback.FeedbackDtos.*;
import vn.starter.zma.security.AuthUser;
import vn.starter.zma.storage.StorageService;
import vn.starter.zma.user.UserRepository;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    public static final int MAX_IMAGES = 5;

    private final FeedbackRepository repo;
    private final UserRepository userRepository;
    private final StorageService storage;
    private final FeedbackMapper mapper;
    private final ApplicationEventPublisher events;

    @Transactional
    public FeedbackDto create(Long userId, CreateFeedbackRequest req, List<MultipartFile> images) {
        List<MultipartFile> files = images == null ? List.of() : images.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (files.size() > MAX_IMAGES) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tối đa " + MAX_IMAGES + " ảnh");
        }
        if (!FeedbackCategories.ALL.contains(req.category())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Chuyên mục không hợp lệ");
        }
        files.forEach(vn.starter.zma.storage.ImageValidator::detectExtension); // validate hết trước khi lưu tệp nào
        Feedback f = new Feedback();
        f.setUser(userRepository.getReferenceById(userId));
        f.setCategory(req.category());
        f.setTitle(req.title().trim());
        f.setContent(req.content().trim());
        f.setAddress(req.address() == null || req.address().isBlank() ? null : req.address().trim());
        f.setLat(req.lat());
        f.setLng(req.lng());
        f.setStatus(FeedbackStatus.NEW);
        for (MultipartFile file : files) {
            f.addImage(storage.storeImage(file, "feedback"));
        }
        repo.saveAndFlush(f);
        f.setCode("PA-%d-%06d".formatted(Year.now().getValue(), f.getId()));
        return mapper.toDto(repo.findDetailById(f.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackSummaryDto> mine(Long userId, int page, int size) {
        return PageResponse.of(repo.findByUserId(userId, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))),
                mapper::toSummary);
    }

    /** Chủ sở hữu hoặc ADMIN mới xem được. Trả 404 thay vì 403 để không lộ sự tồn tại của bản ghi. */
    @Transactional(readOnly = true)
    public FeedbackDto get(Long id, AuthUser viewer) {
        Feedback f = repo.findDetailById(id).orElseThrow(() -> AppException.notFound("phản ánh"));
        if (!viewer.isAdmin() && !f.getUser().getId().equals(viewer.id())) {
            throw AppException.notFound("phản ánh");
        }
        return mapper.toDto(f);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackSummaryDto> search(String q, FeedbackStatus status, int page, int size) {
        return PageResponse.of(repo.search(PageRequests.likePattern(q), status,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))), mapper::toSummary);
    }

    @Transactional
    public FeedbackDto update(Long id, UpdateFeedbackRequest req) {
        Feedback f = repo.findDetailById(id).orElseThrow(() -> AppException.notFound("phản ánh"));
        boolean statusChanged = f.getStatus() != req.status();
        boolean replyChanged = req.adminReply() != null && !req.adminReply().equals(f.getAdminReply());
        f.setStatus(req.status());
        if (req.adminReply() != null) f.setAdminReply(req.adminReply().isBlank() ? null : req.adminReply().trim());
        if (statusChanged || replyChanged) {
            events.publishEvent(new FeedbackStatusChangedEvent(f.getId(), f.getUser().getId(), f.getUser().getPhone(),
                    f.getCode(), f.getTitle(), f.getStatus(), f.getAdminReply()));
        }
        return mapper.toDto(f);
    }
}
