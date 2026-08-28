package vn.nguongocso.farm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.InputMaterial;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.InputMaterialRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.HarvestEligibilityService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Triển khai dịch vụ tính toán điều kiện cách ly thu hoạch (NCL-681 / NCL-843).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HarvestEligibilityServiceImpl implements HarvestEligibilityService {

    private final ProductionLotRepository productionLotRepository;
    private final FarmLogRepository farmLogRepository;
    private final InputMaterialRepository inputMaterialRepository;

    @Override
    public HarvestEligibilityResponse calculateHarvestEligibility(UUID productionLotId) {
        if (productionLotId == null || !productionLotRepository.existsById(productionLotId)) {
            throw new BusinessException("Không tìm thấy lô sản xuất");
        }

        List<FarmLog> pesticideLogs = farmLogRepository
                .findByProductionLotIdAndActivityType(productionLotId, FarmActivityType.PESTICIDE);

        // Trường hợp lô chưa từng có nhật ký PESTICIDE nào
        if (pesticideLogs == null || pesticideLogs.isEmpty()) {
            return HarvestEligibilityResponse.builder()
                    .determined(true)
                    .eligibleHarvestDate(null)
                    .unmatchedMaterials(Collections.emptyList())
                    .build();
        }

        boolean allResolved = true;
        LocalDate maxEligibleDate = null;
        Set<String> unmatchedSet = new LinkedHashSet<>();

        for (FarmLog logItem : pesticideLogs) {
            // Kiểm tra executedDate: nếu thiếu ngày thực hiện thì không thể xác định an toàn, bắt buộc bổ sung trước (B-02)
            if (logItem.getExecutedDate() == null) {
                throw new BusinessException("Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện. Vui lòng bổ sung ngày trước khi thu hoạch.");
            }

            String rawMaterial = logItem.getMaterial();
            if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
                allResolved = false;
                unmatchedSet.add("(Chưa đặt tên)");
                continue;
            }

            String trimmedMaterial = rawMaterial.trim();
            List<InputMaterial> matches = inputMaterialRepository.findByNameNormalized(trimmedMaterial);

            if (matches == null || matches.isEmpty()) {
                allResolved = false;
                unmatchedSet.add(trimmedMaterial);
            } else {
                InputMaterial matchedMaterial = matches.get(0);
                int quarantineDays = matchedMaterial.getQuarantineDays() != null
                        ? matchedMaterial.getQuarantineDays()
                        : 0;

                LocalDate eligibleDateForThisLog = logItem.getExecutedDate().plusDays(quarantineDays);
                if (maxEligibleDate == null || eligibleDateForThisLog.isAfter(maxEligibleDate)) {
                    maxEligibleDate = eligibleDateForThisLog;
                }
            }
        }

        if (!allResolved) {
            return HarvestEligibilityResponse.builder()
                    .determined(false)
                    .eligibleHarvestDate(null)
                    .unmatchedMaterials(new ArrayList<>(unmatchedSet))
                    .build();
        }

        return HarvestEligibilityResponse.builder()
                .determined(true)
                .eligibleHarvestDate(maxEligibleDate)
                .unmatchedMaterials(Collections.emptyList())
                .build();
    }
}
