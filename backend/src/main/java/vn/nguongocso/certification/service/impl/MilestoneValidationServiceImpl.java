package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.ProductCategoryMilestone;
import vn.nguongocso.certification.repository.ProductCategoryMilestoneRepository;
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

    private final ProductCategoryMilestoneRepository categoryMilestoneRepository;
    private final FarmLogRepository farmLogRepository;

    @Override
    public List<String> validateMilestoneCompletion(ProductionLot lot) {
        UUID categoryId = lot.getProductCategory().getId();

        // Step 1: Get standard_ids from lot's certifications
        List<UUID> standardIds = lot.getCertifications().stream()
                .map(ProductionLotCertification::getCertification)
                .filter(cert -> cert.getStandard() != null)
                .map(cert -> cert.getStandard().getId())
                .distinct()
                .toList();

        // Step 2: Query mandatory milestones for this category + standard scope
        List<ProductCategoryMilestone> mandatoryMilestones =
                categoryMilestoneRepository.findMandatoryMilestonesForValidation(categoryId, standardIds);

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
        List<String> missingMilestones = new ArrayList<>();

        for (ProductCategoryMilestone pcm : mandatoryMilestones) {
            CultivationMilestoneCatalog milestone = pcm.getMilestone();
            FarmActivityType activityType;
            try {
                activityType = FarmActivityType.valueOf(milestone.getActivityType());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown activity type '{}' in milestone {}", milestone.getActivityType(), milestone.getId());
                missingMilestones.add(milestone.getName());
                continue;
            }

            long availableLogs = logCountsByType.getOrDefault(activityType, 0L);
            int usedCount = usedCountsByType.getOrDefault(activityType, 0);

            if (usedCount < (int) availableLogs) {
                // This milestone is satisfied by one farm log
                usedCountsByType.put(activityType, usedCount + 1);
            } else {
                // No more farm logs available for this activity type
                missingMilestones.add(milestone.getName());
            }
        }

        return missingMilestones;
    }
}
