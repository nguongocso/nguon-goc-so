package vn.nguongocso.integration.partner.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Service xử lý lấy hồ sơ truy xuất lô sản xuất cho bên thứ ba (NCL-12-CN-002).
 * <p>
 * Đảm bảo quy tắc bảo mật Cách ly Dữ liệu Tổ chức (Tenant Isolation - TC-04).
 */
@Service
@RequiredArgsConstructor
public class PartnerLotService {

    private static final Logger log = LoggerFactory.getLogger(PartnerLotService.class);

    private final ProductionLotRepository productionLotRepository;
    private final FarmLogRepository farmLogRepository;

    /**
     * Lấy hồ sơ truy xuất đầy đủ của lô sản xuất cho bên thứ ba.
     * <p>
     * Thực thi quy tắc Cách ly Dữ liệu Tổ chức (Tenant Isolation - TC-04):
     * Chỉ cho phép truy xuất lô thuộc sở hữu của Hợp tác xã tương ứng với PartnerApiKey.
     */
    @Transactional(readOnly = true)
    public PartnerLotDossierResponse getLotDossierForPartner(UUID lotId, PartnerApiKey partnerApiKey) {
        if (partnerApiKey == null || partnerApiKey.getOrganization() == null) {
            throw new BusinessException("Khóa truy cập không hợp lệ hoặc thiếu thông tin tổ chức");
        }

        UUID organizationId = partnerApiKey.getOrganization().getOrganizationId();

        // 1. Kiểm tra lô sản xuất tồn tại không
        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin lô sản xuất"));

        // 2. Kiểm tra Cách ly dữ liệu Tổ chức (TC-04): Lô có thuộc HTX của API Key hay không
        if (!lot.getOrganization().getOrganizationId().equals(organizationId)) {
            log.warn("Bên thứ ba '{}' (orgId={}) cố tình truy cập lô {} thuộc orgId khác={}",
                    partnerApiKey.getPartnerName(), organizationId, lotId, lot.getOrganization().getOrganizationId());
            throw new BusinessException("Lô sản xuất nằm ngoài phạm vi truy xuất của khóa truy cập");
        }

        // Truy vấn nạp đủ quan hệ phục vụ response
        ProductionLot fullLot = productionLotRepository.findDossierByIdAndOrganizationId(lotId, organizationId)
                .orElse(lot);

        return mapToDossierResponse(fullLot);
    }

    private PartnerLotDossierResponse mapToDossierResponse(ProductionLot lot) {
        // 1. Lot Info
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

        // 2. Org Info
        Organization org = lot.getOrganization();
        PartnerOrgInfoResponse orgInfo = PartnerOrgInfoResponse.builder()
                .organizationId(org.getOrganizationId().toString())
                .organizationName(org.getName())
                .organizationCode(org.getCode())
                .address(org.getAddress())
                .phone(org.getPhone())
                .email(org.getEmail())
                .build();

        // 3. Farm Area Info
        PartnerFarmAreaResponse farmAreaInfo = null;
        if (lot.getFarmArea() != null) {
            FarmArea fa = lot.getFarmArea();
            farmAreaInfo = PartnerFarmAreaResponse.builder()
                    .farmAreaId(fa.getId().toString())
                    .farmAreaName(fa.getName())
                    .acreageM2(fa.getAcreageM2())
                    .address(fa.getAddress())
                    .build();
        }

        // 4. Certifications
        List<PartnerCertificationResponse> certResponses = new ArrayList<>();
        if (lot.getCertifications() != null) {
            for (ProductionLotCertification plc : lot.getCertifications()) {
                Certification cert = plc.getCertification();
                if (cert != null) {
                    certResponses.add(PartnerCertificationResponse.builder()
                            .certificationName(cert.getName())
                            .standardCode(cert.getStandard() != null ? cert.getStandard().getCode() : null)
                            .certificateNumber(cert.getCertificateNumber())
                            .issuedDate(cert.getIssuedDate())
                            .expiredDate(cert.getExpiredDate())
                            .issuingBody(cert.getIssuingBody())
                            .build());
                }
            }
        }

        // 5. Farm Log Summary
        int logCount = 0;
        try {
            var logs = farmLogRepository.findByProductionLotId(lot.getId());
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
                .build();
    }
}
