package vn.nguongocso.notification.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.enums.NotificationType;

/**
 * Chuyển đổi cột notifications.type (chuỗi) sang NotificationType.
 */
@Slf4j
@Converter
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    /** Lưu loại thông báo dưới dạng tên enum. */
    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute == null ? null : attribute.name();
    }

    /** Đọc loại thông báo từ DB, giá trị lạ quy về INFO. */
    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            log.warn("Thông báo có loại rỗng trong DB, tạm hiển thị dưới dạng INFO.");
            return NotificationType.INFO;
        }

        try {
            return NotificationType.valueOf(dbData.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Loại thông báo '{}' không có trong NotificationType, tạm hiển thị dưới dạng INFO.", dbData);
            return NotificationType.INFO;
        }
    }
}