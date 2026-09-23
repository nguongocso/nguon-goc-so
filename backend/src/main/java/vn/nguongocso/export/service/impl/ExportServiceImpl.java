package vn.nguongocso.export.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.exception.TemplateNotOwnedException;
import vn.nguongocso.export.repository.ProfileTemplateRepository;
import vn.nguongocso.export.service.ExportService;
import vn.nguongocso.export.service.ProfileTemplateService;
import vn.nguongocso.export.service.processor.OpenDataExportProcessor;
import vn.nguongocso.export.service.recorder.ExportLogRecorder;
import vn.nguongocso.export.service.renderer.ExportCsvRenderer;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Điều phối dịch vụ xuất dữ liệu công khai và xuất hồ sơ theo mẫu đối tác.
 * Sử dụng mô hình Orchestrator không giữ transaction trong quá trình render tệp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final ShipmentRepository shipmentRepository;
    private final ProfileTemplateRepository profileTemplateRepository;
    private final ProfileTemplateService profileTemplateService;
    private final OpenDataExportProcessor openDataExportProcessor;
    private final ExportCsvRenderer exportCsvRenderer;
    private final ExportLogRecorder exportLogRecorder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Resource exportOpenData(ExportOpenDataRequest request, CustomUserDetails currentUser) {
        var schema = openDataExportProcessor.buildSnapshot(request, currentUser);
        return openDataExportProcessor.generateFile(schema, request.getFormat());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Resource exportWithTemplate(UUID shipmentId, UUID templateId, String format, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        validateShipmentOwnership(shipment, currentUser);
        ProfileTemplate effectiveTemplate = resolveEffectiveTemplate(shipment, templateId, currentUser);

        // 1. Lấy dữ liệu xem trước đã materialize 100% qua read-only transaction độc lập
        var previewData = profileTemplateService.buildPreview(shipmentId, templateId, currentUser);

        // 2. Render nội dung tệp hoàn toàn ngoài transaction (in-memory CPU)
        byte[] fileBytes = renderExportBytes(previewData, format);

        // 3. Nếu render thành công, mở write-transaction ngắn độc lập để lưu ExportLog
        exportLogRecorder.recordExportLog(shipment, effectiveTemplate, currentUser.getUserId());

        return new ByteArrayResource(fileBytes);
    }

    private void validateShipmentOwnership(Shipment shipment, CustomUserDetails currentUser) {
        UUID userOrgId = currentUser.getOrganizationId();
        if ("VT-02".equals(currentUser.getRoleCode())) {
            if (shipment.getOrganization() == null
                    || !shipment.getOrganization().getOrganizationId().equals(userOrgId)) {
                throw new TemplateNotOwnedException("Từ chối thao tác: Lô hàng không thuộc tổ chức của bạn.");
            }
        }
    }

    private ProfileTemplate resolveEffectiveTemplate(
            Shipment shipment,
            UUID templateId,
            CustomUserDetails currentUser) {
        UUID userOrgId = currentUser.getOrganizationId();
        UUID effectiveOrgId = ("VT-04".equals(currentUser.getRoleCode()) && shipment.getOrganization() != null)
                ? shipment.getOrganization().getOrganizationId()
                : userOrgId;

        if (templateId != null) {
            ProfileTemplate template = profileTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));
            if ("VT-04".equals(currentUser.getRoleCode())) {
                if (!template.getOrganization().getOrganizationId().equals(effectiveOrgId)) {
                    throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của lô hàng này.");
                }
            } else {
                if (!template.getOrganization().getOrganizationId().equals(userOrgId)) {
                    throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
                }
            }
            return template;
        }

        return profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(effectiveOrgId)
                .orElse(null);
    }

    private byte[] renderExportBytes(Object previewData, String format) {
        if ("csv".equalsIgnoreCase(format)) {
            @SuppressWarnings("unchecked")
            var mapData = (java.util.Map<String, Object>) previewData;
            String csvContent = exportCsvRenderer.renderPreviewToCsv(mapData);
            return csvContent.getBytes(StandardCharsets.UTF_8);
        }

        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(previewData);
            return jsonContent.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("Lỗi khi chuyển đổi dữ liệu hồ sơ sang JSON: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi khi tạo file xuất JSON: " + e.getMessage());
        }
    }
}
