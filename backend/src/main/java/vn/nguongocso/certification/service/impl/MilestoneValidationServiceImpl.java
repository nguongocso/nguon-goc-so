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
 * Implementation of MilestoneValidationService.
 * Validates that all mandatory milestones are satisfied by farm logs before packaging.
 * Story: NCL-09-CN-011
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MilestoneValidationServiceImpl implements MilestoneValidationService {

    private final CultivationMilestoneRepository milestoneRepository;
    private final FarmLogRepository farmLogRepository;

    @Override
    public MilestoneValidationResult validateMilestoneCompletion(ProductionLot lot) {
        List<String> missingMilestones = findMissingMilestones(lot).stream()
                .map(CultivationMilestone::getName)
                .toList();

        if (!missingMilestones.isEmpty()) {
            return MilestoneValidationResult.builder()
                    .eligible(false)
                    .missingMilestones(missingMilestones)
                    .build();
        }

        // Check if the lot has ANY farm logs at all
        long farmLogCount = farmLogRepository.countByProductionLotId(lot.getId());
        if (farmLogCount == 0) {
            List<FarmLog> logs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId());
            if (logs != null && !logs.isEmpty()) {
                farmLogCount = logs.size();
            }
        }
        if (farmLogCount == 0) {
            log.warn("Lot {} has no farm logs. Packaging may proceed but this should be reviewed.", lot.getId());
            // Return false with a warning message (do not block, just warn)
            return MilestoneValidationResult.builder()
                    .eligible(false)
                    .message("Lô chưa có nhật ký canh tác. Vui lòng ghi nhật ký trước khi đóng gói.")
                    .build();
        }

        return MilestoneValidationResult.builder()
                .eligible(true)
                .missingMilestones(List.of())
                .build();
    }

    @Override
    public List<CultivationMilestone> findMissingMilestones(ProductionLot lot) {
        UUID categoryId = lot.getProductCategory().getId();

        // Step 1: Get standard_ids from lot's certifications
        List<UUID> standardIds = lot.getCertifications().stream()
                .map(ProductionLotCertification::getCertification)
                .filter(cert -> cert.getStandard() != null)
                .map(cert -> cert.getStandard().getId())
                .distinct()
                .toList();

        // Step 2: Query mandatory milestones for this category + standard scope
        List<CultivationMilestone> mandatoryMilestones =
                milestoneRepository.findMandatoryMilestonesForValidation(categoryId, standardIds);

        if (mandatoryMilestones.isEmpty()) {
            return List.of();
        }

        // Step 3: Get active (non-corrected) farm logs for this lot
        List<FarmLog> farmLogs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId());
        List<FarmLog> activeLogs = farmLogs.stream()
                .filter(fl -> !fl.isCorrected())
                .toList();

        // Step 4: Count active logs per activity type
        Map<FarmActivityType, Long> logCountsByType = activeLogs.stream()
                .collect(Collectors.groupingBy(FarmLog::getActivityType, Collectors.counting()));

        // Step 5: Match required milestones to farm logs (1:1)
        // Each required milestone needs exactly one FarmLog with matching activity_type
        Map<FarmActivityType, Integer> usedCountsByType = new java.util.HashMap<>();
        List<CultivationMilestone> missingMilestones = new ArrayList<>();

        for (CultivationMilestone milestone : mandatoryMilestones) {
            FarmActivityType activityType;
            try {
                activityType = FarmActivityType.valueOf(milestone.getActivityType());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown activity type '{}' in milestone {}", milestone.getActivityType(), milestone.getId());
                missingMilestones.add(milestone);
                continue;
            }

            long availableLogs = logCountsByType.getOrDefault(activityType, 0L);
            int usedCount = usedCountsByType.getOrDefault(activityType, 0);

            if (usedCount < (int) availableLogs) {
                // This milestone is satisfied by one farm log
                usedCountsByType.put(activityType, usedCount + 1);
            } else {
                // No more farm logs available for this activity type
                missingMilestones.add(milestone);
            }
        }

        return missingMilestones;
    }
}
