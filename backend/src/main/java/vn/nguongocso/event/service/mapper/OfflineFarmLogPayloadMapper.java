package vn.nguongocso.event.service.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.stereotype.Component;

import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Component chuyên trách phân tích và chuyển đổi payload sự kiện ngoại tuyến FARM_LOG
 * sang CreateFarmLogRequest cho dịch vụ nhật ký canh tác.
 */
@Component
public class OfflineFarmLogPayloadMapper {
    /**
     * Chuyển đổi và xác thực sự kiện ngoại tuyến FARM_LOG sang CreateFarmLogRequest.
     *
     * @param eventDto sự kiện ngoại tuyến
     * @return CreateFarmLogRequest hợp lệ
     */
    public CreateFarmLogRequest toCreateFarmLogRequest(RecordOfflineEventDto eventDto) {
        validateFarmLogBasics(eventDto);
        Map<String, Object> eventData = eventDto.getEventData();
        FarmActivityType activityType = extractAndValidateActivityType(eventData);
        LocalDate executedDate = extractAndValidateExecutedDate(eventData);

        CreateFarmLogRequest farmLogRequest = new CreateFarmLogRequest();
        farmLogRequest.setProductionLotId(eventDto.getProductionLotId());
        farmLogRequest.setActivityType(activityType);
        farmLogRequest.setExecutedDate(executedDate);

        applyOptionalFarmLogFields(eventData, farmLogRequest);
        return farmLogRequest;
    }

    private void validateFarmLogBasics(RecordOfflineEventDto eventDto) {
        if (eventDto.getProductionLotId() == null) {
            throw new BusinessException("Vui lòng chọn lô sản xuất");
        }
        if (eventDto.getEventData() == null) {
            throw new BusinessException("Thiếu dữ liệu nhật ký canh tác.");
        }
    }

    private FarmActivityType extractAndValidateActivityType(Map<String, Object> eventData) {
        Object activityTypeObj = eventData.get("activityType");
        if (activityTypeObj == null || activityTypeObj.toString().isBlank()) {
            throw new BusinessException("Vui lòng chọn loại hoạt động");
        }
        try {
            return FarmActivityType.valueOf(activityTypeObj.toString().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Loại hoạt động không hợp lệ: " + activityTypeObj);
        }
    }

    private LocalDate extractAndValidateExecutedDate(Map<String, Object> eventData) {
        Object executedDateObj = eventData.get("executedDate");
        if (executedDateObj == null || executedDateObj.toString().isBlank()) {
            throw new BusinessException("Vui lòng chọn ngày thực hiện");
        }
        try {
            return LocalDate.parse(executedDateObj.toString().trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Ngày thực hiện không hợp lệ: " + executedDateObj);
        }
    }

    private void applyOptionalFarmLogFields(Map<String, Object> eventData, CreateFarmLogRequest farmLogRequest) {
        String material = getOptionalText(eventData, "material");
        if (material != null) {
            farmLogRequest.setMaterial(material);
        }
        applyQuantityField(eventData, farmLogRequest);
        String unit = getOptionalText(eventData, "unit");
        if (unit != null) {
            farmLogRequest.setUnit(unit);
        }
        String notes = getOptionalText(eventData, "notes");
        if (notes != null) {
            farmLogRequest.setNotes(notes);
        }
        applyMilestoneField(eventData, farmLogRequest);
    }

    private void applyQuantityField(Map<String, Object> eventData, CreateFarmLogRequest farmLogRequest) {
        Object quantityObj = eventData.get("quantity");
        if (quantityObj == null || quantityObj.toString().isBlank()) {
            return;
        }
        try {
            farmLogRequest.setQuantity(Double.valueOf(quantityObj.toString()));
        } catch (NumberFormatException e) {
            throw new BusinessException("Số lượng phải là số.");
        }
    }

    private void applyMilestoneField(Map<String, Object> eventData, CreateFarmLogRequest farmLogRequest) {
        Object milestoneObj = eventData.get("milestoneId");
        if (milestoneObj == null || milestoneObj.toString().isBlank()) {
            return;
        }
        try {
            farmLogRequest.setMilestoneId(Long.valueOf(milestoneObj.toString()));
        } catch (NumberFormatException e) {
            throw new BusinessException("ID mốc canh tác không hợp lệ.");
        }
    }

    private String getOptionalText(Map<String, Object> eventData, String key) {
        Object value = eventData.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }
}
