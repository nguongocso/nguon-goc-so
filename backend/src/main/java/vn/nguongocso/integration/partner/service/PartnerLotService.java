package vn.nguongocso.integration.partner.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.dto.response.PartnerCertificationResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerFarmAreaResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerFarmLogSummaryResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerLotDossierResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerLotInfoResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerOrgInfoResponse;
import vn.nguongocso.organization.entity.Organization;

/**
 * Service xử lý lấy hồ sơ truy xuất lô sản xuất cho bên thứ ba.
*/
@Service
@RequiredArgsConstructor
public class PartnerLotService {
    private static final Logger log = LoggerFactory.getLogger(PartnerLotService.class);

    private final ProductionLotRepository productionLotRepository;
    private final FarmLogRepository farmLogRepository;

    /**
     * Lấy hồ sơ truy xuất đầy đủ của lô sản xuất cho bên thứ ba.
     */
    @Transactional(readOnly = true)
    public PartnerLotDossierResponse getLotDossierForPartner(UUID lotId, PartnerApiKey partnerApiKey) {
        if (partnerApiKey == null || partnerApiKey.getOrganization() == null) {
            throw new BusinessException("Khóa truy cập không hợp lệ hoặc thiếu thông tin tổ chức");
        }

        if (Boolean.TRUE.equals(partnerApiKey.getIsTest())
                || (partnerApiKey.getKeyPrefix() != null && partnerApiKey.getKeyPrefix().startsWith("nks_test_"))) {
            log.warn(
                    "Bên thứ ba '{}' dùng khóa thử nghiệm gọi lấy hồ sơ lô thực tế (lotId={})",
                    partnerApiKey.getPartnerName(), lotId);
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Khóa thử nghiệm chỉ được phép truy cập mã lô \"sample-lot-001\". Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa API thật.");
        }

        UUID organizationId = partnerApiKey.getOrganization().getOrganizationId();

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy lô sản xuất yêu cầu"));

        if (!lot.getOrganization().getOrganizationId().equals(organizationId)) {
            log.warn("Bên thứ ba '{}' (orgId={}) cố tình truy cập lô {} thuộc orgId khác={}",
                    partnerApiKey.getPartnerName(), organizationId, lotId, lot.getOrganization().getOrganizationId());
            throw new BusinessException("Lô sản xuất nằm ngoài phạm vi truy xuất của khóa truy cập");
        }

        ProductionLot fullLot = productionLotRepository.findDossierByIdAndOrganizationId(lotId, organizationId)
                .orElse(lot);

        return mapToDossierResponse(fullLot);
    }

    /**
     * Chuyển đổi entity lô sản xuất sang response hồ sơ truy xuất.
     */
    private PartnerLotDossierResponse mapToDossierResponse(ProductionLot lot) {
        PartnerLotInfoResponse lotInfo = PartnerLotInfoResponse.builder()
                .lotId(lot.getId().toString())
                .lotName(lot.getName())
                .productCategoryName(lot.getProductCategory() != null ? lot.getProductCategory().getName() : null)
                .expectedQuantity(lot.getExpectedQuantity())
                .actualQuantity(lot.getActualQuantity())
                .quantityUnit(lot.getExpectedQuantityUnit())
                .plantingDate(lot.getPlantingDate())
                .harvestDate(lot.getHarvestDate())
                .status(lot.getStatus())
                .build();

        Organization org = lot.getOrganization();
        PartnerOrgInfoResponse orgInfo = PartnerOrgInfoResponse.builder()
                .organizationId(org.getOrganizationId().toString())
                .organizationName(org.getName())
                .organizationCode(org.getCode())
                .address(org.getAddress())
                .phone(org.getPhone())
                .email(org.getEmail())
                .build();

        PartnerFarmAreaResponse farmAreaInfo = null;
        if (lot.getFarmArea() != null) {
            FarmArea fa = lot.getFarmArea();
            farmAreaInfo = PartnerFarmAreaResponse.builder()
                    .farmAreaId(fa.getId().toString())
                    .farmAreaName(fa.getName())
                    .area(fa.getArea() != null ? fa.getArea().doubleValue() : null)
                    .areaUnit(fa.getAreaUnit() != null ? fa.getAreaUnit().name() : null)
                    .build();
        }

        List<PartnerCertificationResponse> certResponses = new ArrayList<>();
        if (lot.getCertifications() != null) {
            for (ProductionLotCertification plc : lot.getCertifications()) {
                Certification cert = plc.getCertification();
                if (cert != null) {
                    certResponses.add(PartnerCertificationResponse.builder()
                            .certificationName(cert.getName())
                            .standardName(cert.getStandard() != null ? cert.getStandard().getName() : null)
                            .certificateCode(cert.getCode())
                            .issueDate(cert.getIssueDate())
                            .expiryDate(cert.getExpiryDate())
                            .issuedBy(cert.getIssuedBy())
                            .build());
                }
            }
        }

        int logCount = 0;
        try {
            var logs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId());
            if (logs != null) {
                logCount = logs.size();
            }
        } catch (Exception e) {
            log.warn("Không thể lấy tóm tắt nhật ký canh tác cho lô {}", lot.getId());
        }

        PartnerFarmLogSummaryResponse logSummary = PartnerFarmLogSummaryResponse.builder()
                .totalLogsRecorded(logCount)
                .lastActivityAt(lot.getUpdatedAt())
                .build();

        return PartnerLotDossierResponse.builder()
                .lotInfo(lotInfo)
                .organizationInfo(orgInfo)
                .farmAreaInfo(farmAreaInfo)
                .certifications(certResponses)
                .farmLogSummary(logSummary)
                .isTest(false)
                .build();
    }
}
