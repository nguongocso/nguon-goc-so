package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.certification.dto.response.MilestoneValidationResult;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.repository.CultivationMilestoneRepository;
import vn.nguongocso.certification.service.MilestoneValidationService;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.FarmLogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai kiểm tra mốc canh tác bắt buộc của lô sản xuất trước khi đóng gói
 * (NCL-09-CN-011).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MilestoneValidationServiceImpl implements MilestoneValidationService {
        private final CultivationMilestoneRepository milestoneRepository;
        private final FarmLogRepository farmLogRepository;

        /**
         * Kiểm tra các mốc canh tác bắt buộc đã được ghi nhận trong nhật ký canh tác
         * của lô hay chưa.
         */
        @Override
        public MilestoneValidationResult validateMilestoneCompletion(ProductionLot lot) {
                List<String> missingMilestones = findMissingMilestones(lot).stream()
                                .map(CultivationMilestone::getName)
                                .toList();

                boolean isEligible = missingMilestones.isEmpty();

                return MilestoneValidationResult.builder()
                                .eligible(isEligible)
                                .missingMilestones(missingMilestones)
                                .message(isEligible ? null : "Lô chưa đủ mốc canh tác bắt buộc trước khi đóng gói.")
                                .build();
        }

        /**
         * Tìm danh sách các mốc canh tác bắt buộc còn thiếu đối với lô sản xuất.
         */
        @Override
        public List<CultivationMilestone> findMissingMilestones(ProductionLot lot) {
                UUID categoryId = lot.getProductCategory().getId();

                List<UUID> standardIds = lot.getCertifications().stream()
                                .map(ProductionLotCertification::getCertification)
                                .filter(cert -> cert.getStandard() != null)
                                .map(cert -> cert.getStandard().getId())
                                .distinct()
                                .toList();

                List<CultivationMilestone> mandatoryMilestones = milestoneRepository
                                .findMandatoryMilestonesForValidation(categoryId, standardIds);

                if (mandatoryMilestones.isEmpty()) {
                        return List.of();
                }

                List<FarmLog> farmLogs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId());
                List<FarmLog> activeLogs = farmLogs.stream()
                                .filter(fl -> !fl.isCorrected())
                                .toList();

                Map<FarmActivityType, Long> logCountsByType = activeLogs.stream()
                                .collect(Collectors.groupingBy(FarmLog::getActivityType, Collectors.counting()));

                Map<FarmActivityType, Integer> usedCountsByType = new java.util.HashMap<>();
                List<CultivationMilestone> missingMilestones = new ArrayList<>();

                for (CultivationMilestone milestone : mandatoryMilestones) {
                        FarmActivityType activityType;
                        try {
                                activityType = FarmActivityType.valueOf(milestone.getActivityType());
                        } catch (IllegalArgumentException e) {
                                log.warn("Loại hoạt động không hợp lệ '{}' trong mốc {}", milestone.getActivityType(),
                                                milestone.getId());
                                missingMilestones.add(milestone);
                                continue;
                        }

                        long availableLogs = logCountsByType.getOrDefault(activityType, 0L);
                        int usedCount = usedCountsByType.getOrDefault(activityType, 0);

                        if (usedCount < (int) availableLogs) {
                                usedCountsByType.put(activityType, usedCount + 1);
                        } else {
                                missingMilestones.add(milestone);
                        }
                }

                return missingMilestones;
        }
}
