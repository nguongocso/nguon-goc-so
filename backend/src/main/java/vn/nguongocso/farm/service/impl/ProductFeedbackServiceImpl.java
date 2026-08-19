package vn.nguongocso.farm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.event.ProductFeedbackSubmittedEvent;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.ProductFeedbackService;
import vn.nguongocso.notification.service.NotificationService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/** Lưu, quản lý và phát sự kiện phản ánh sản phẩm. */
public class ProductFeedbackServiceImpl implements ProductFeedbackService {

        private static final Logger log = LoggerFactory.getLogger(ProductFeedbackServiceImpl.class);
        private static final String ADMIN_ROLE = "VT-01";

        private final ProductFeedbackRepository productFeedbackRepository;
        private final ProductionLotRepository productionLotRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final NotificationService notificationService;

        /** Tạo phản ánh mới cho lô sản xuất (public). */
        @Override
        @Transactional
        public ProductFeedbackResponse createFeedback(UUID productionLotId, CreateProductFeedbackRequest request) {
                log.info("Bắt đầu xử lý gửi phản ánh sản phẩm cho lô sản xuất ID: {}", productionLotId);

                // 1. Kiểm tra sự tồn tại của lô sản xuất
                ProductionLot productionLot = productionLotRepository.findById(productionLotId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô sản xuất"));

                // 2. Tạo entity ProductFeedback
                ProductFeedback feedback = ProductFeedback.builder()
                                .productionLot(productionLot)
                                .content(request.getContent())
                                .build();

                // 3. Lưu vào cơ sở dữ liệu
                ProductFeedback savedFeedback = productFeedbackRepository.save(feedback);
                log.info("Đã lưu thành công phản ánh sản phẩm ID: {}", savedFeedback.getId());

                // 4. Phát sự kiện ProductFeedbackSubmittedEvent
                UUID orgId = productionLot.getOrganization() != null
                                ? productionLot.getOrganization().getOrganizationId()
                                : null;
                String orgName = productionLot.getOrganization() != null
                                ? productionLot.getOrganization().getName()
                                : null;
                ProductFeedbackSubmittedEvent event = new ProductFeedbackSubmittedEvent(
                                this,
                                savedFeedback.getId(),
                                productionLot.getId(),
                                productionLot.getName(),
                                orgId,
                                savedFeedback.getContent());
                eventPublisher.publishEvent(event);
                log.info("Đã phát sự kiện ProductFeedbackSubmittedEvent cho phản ánh ID: {}", savedFeedback.getId());

                // 5. Gửi thông báo đến VT-01 và VT-02 của tổ chức
                try {
                        notificationService.sendAlert(String.format(
                                        "Phản ánh mới về lô sản xuất \"%s\" (ID: %s) từ tổ chức \"%s\". Nội dung: \"%s\"",
                                        productionLot.getName(),
                                        productionLot.getId(),
                                        orgName != null ? orgName : "N/A",
                                        savedFeedback.getContent()));
                        log.info("Đã gửi cảnh báo phản ánh sản phẩm ID: {}", savedFeedback.getId());
                } catch (Exception e) {
                        log.warn("Không thể gửi thông báo phản ánh sản phẩm: {}", e.getMessage());
                }

                // 6. Ánh xạ trả về Response DTO
                return mapToResponse(savedFeedback);
        }

        /** Lấy danh sách phản ánh (phân trang) cho nội bộ. */
        @Override
        @Transactional(readOnly = true)
        public PageResponse<ProductFeedbackResponse> getFeedbacks(Pageable pageable) {
                CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
                String roleCode = currentUser.getRoleCode();

                Page<ProductFeedback> page;
                if (ADMIN_ROLE.equals(roleCode)) {
                        // VT-01 sees all feedbacks
                        page = productFeedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
                } else {
                        // VT-02 sees only feedbacks of their organization
                        UUID orgId = currentUser.getOrganizationId();
                        page = productFeedbackRepository
                                        .findByProductionLot_Organization_OrganizationIdOrderByCreatedAtDesc(orgId,
                                                        pageable);
                }

                List<ProductFeedbackResponse> items = page.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList();

                return PageResponse.from(page, items);
        }

        /** Lấy chi tiết một phản ánh. */
        @Override
        @Transactional(readOnly = true)
        public ProductFeedbackResponse getFeedbackById(UUID feedbackId) {
                ProductFeedback feedback = productFeedbackRepository.findById(feedbackId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản ánh"));

                return mapToResponse(feedback);
        }

        private ProductFeedbackResponse mapToResponse(ProductFeedback feedback) {
                ProductionLot lot = feedback.getProductionLot();
                return ProductFeedbackResponse.builder()
                                .id(feedback.getId())
                                .productionLotId(lot.getId())
                                .productionLotName(lot.getName())
                                .content(feedback.getContent())
                                .createdAt(feedback.getCreatedAt())
                                .organizationId(lot.getOrganization() != null
                                                ? lot.getOrganization().getOrganizationId()
                                                : null)
                                .organizationName(lot.getOrganization() != null
                                                ? lot.getOrganization().getName()
                                                : null)
                                .productCategoryName(lot.getProductCategory() != null
                                                ? lot.getProductCategory().getName()
                                                : null)
                                .build();
        }
}
