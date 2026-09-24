package vn.nguongocso.ai.util;

import java.util.List;
import java.util.Locale;

/**
 * Tiện ích nhận diện ý định (Intent Detection) và phát hiện truy vấn tổ chức chéo từ câu hỏi (TASK-AI-06, TASK-AI-07).
 */
public final class AiIntentDetector {

    private static final List<String> ANALYTICS_KEYWORDS = List.of(
            "thống kê", "thong ke",
            "bao nhiêu lô", "bao nhieu lo",
            "số lô", "so lo",
            "sản lượng", "san luong",
            "thu hoạch", "thu hoach",
            "sắp hết hạn", "sap het han",
            "chứng nhận", "chung nhan",
            "vietgap", "globalgap", "hết hạn", "het han",
            "cảnh báo", "canh bao",
            "bất thường", "bat thuong",
            "thu hồi", "thu hoi",
            "vi phạm", "vi pham",
            "lô hàng", "lo hang",
            "bàn giao", "ban giao",
            "vận chuyển", "van chuyen",
            "tiếp nhận", "tiep nhan",
            "diện tích", "dien tich",
            "tình hình", "tinh hinh",
            "báo cáo", "bao cao",
            "bao nhiêu ha", "bao nhieu ha",
            "tổng số", "tong so"
    );

    private static final List<String> CROSS_ORG_KEYWORDS = List.of(
            "htx khác", "htx khac",
            "tổ chức khác", "to chuc khac",
            "doanh nghiệp khác", "doanh nghiep khac",
            "của htx", "cua htx",
            "của tổ chức", "cua to chuc"
    );

    private AiIntentDetector() {
    }

    /**
     * Nhận diện câu hỏi có chứa ý định tra cứu / thống kê số liệu nghiệp vụ hay không.
     *
     * @param message Tin nhắn người dùng
     * @return true nếu có ý định thống kê dữ liệu
     */
    public static boolean hasAnalyticsIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        for (String keyword : ANALYTICS_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem người dùng có đang cố tình hỏi thông tin của một tổ chức khác hay không.
     *
     * @param message        Tin nhắn người dùng
     * @param currentOrgName Tên tổ chức hiện tại của người dùng
     * @param currentOrgCode Mã tổ chức hiện tại của người dùng
     * @return true nếu phát hiện ý định hỏi dữ liệu tổ chức khác
     */
    public static boolean hasCrossOrgInquiryIntent(String message, String currentOrgName, String currentOrgCode) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);

        for (String crossKeyword : CROSS_ORG_KEYWORDS) {
            if (normalized.contains(crossKeyword)) {
                return true;
            }
        }

        return false;
    }
}
