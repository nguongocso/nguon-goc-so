package vn.nguongocso.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.nguongocso.alert.enums.NotificationType;

/**
 * Kiểm thử đơn vị cho NotificationTypeConverter.
 */
class NotificationTypeConverterTest {

    private final NotificationTypeConverter converter = new NotificationTypeConverter();

    @Test
    @DisplayName("Đọc loại thông báo hợp lệ từ DB")
    void convertToEntityAttribute_returnsEnumValue() {
        assertThat(converter.convertToEntityAttribute("FARM_LOG_SYNC_SUCCESS"))
                .isEqualTo(NotificationType.FARM_LOG_SYNC_SUCCESS);
        assertThat(converter.convertToEntityAttribute(" ALERT "))
                .isEqualTo(NotificationType.ALERT);
    }

    @Test
    @DisplayName("Đọc loại thông báo lạ từ DB: quy về INFO, không ném lỗi")
    void convertToEntityAttribute_whenUnknownValue_returnsInfo() {
        assertThat(converter.convertToEntityAttribute("LEGACY_UNKNOWN_TYPE"))
                .isEqualTo(NotificationType.INFO);
    }

    @Test
    @DisplayName("Đọc loại thông báo rỗng từ DB: quy về INFO")
    void convertToEntityAttribute_whenBlank_returnsInfo() {
        assertThat(converter.convertToEntityAttribute(null)).isEqualTo(NotificationType.INFO);
        assertThat(converter.convertToEntityAttribute("  ")).isEqualTo(NotificationType.INFO);
    }

    @Test
    @DisplayName("Ghi loại thông báo xuống DB dưới dạng tên enum")
    void convertToDatabaseColumn_writesEnumName() {
        assertThat(converter.convertToDatabaseColumn(NotificationType.FARM_LOG_SYNC_FAILED))
                .isEqualTo("FARM_LOG_SYNC_FAILED");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }
}