package vn.nguongocso.report.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.report.dto.response.AlertLotDetailResponse;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.enums.LotAlertType;

/**
 * Service quản lý danh sách và chi tiết lô có cảnh báo theo địa bàn cho cán bộ quản lý ngành (NCL-07-CN-006).
 */
public interface TerritoryLotAlertService {

    /**
     * Lấy danh sách lô có cảnh báo theo địa bàn của người dùng và các bộ lọc.
     *
     * @param currentUser Người dùng đang đăng nhập (VT-05 hoặc VT-01)
     * @param alertType   Loại cảnh báo cần lọc (nullable)
     * @param organizationId Tổ chức cần lọc (nullable)
     * @param fromDate    Thời điểm bắt đầu phát sinh cảnh báo (nullable)
     * @param toDate      Thời điểm kết thúc phát sinh cảnh báo (nullable)
     * @param unitIds     Danh sách địa bàn thu hẹp (nullable/empty)
     * @param pageable    Thông tin phân trang và sắp xếp
     * @return Trang danh sách lô có cảnh báo
     */
    PageResponse<AlertLotSummaryResponse> getAlertLots(
            CustomUserDetails currentUser,
            LotAlertType alertType,
            UUID organizationId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            List<UUID> unitIds,
            Pageable pageable);

    /**
     * Lấy chi tiết thông tin lô có cảnh báo cùng bằng chứng và dòng sự kiện read-only.
     *
     * @param currentUser Người dùng đang đăng nhập (VT-05 hoặc VT-01)
     * @param lotId       ID của lô sản xuất
     * @return Chi tiết lô có cảnh báo
     */
    AlertLotDetailResponse getAlertLotDetail(
            CustomUserDetails currentUser,
            UUID lotId);

    /**
     * Xuất file Excel danh sách lô có cảnh báo theo đúng phạm vi địa bàn và bộ lọc.
     *
     * @param currentUser Người dùng đang đăng nhập
     * @param alertType   Loại cảnh báo cần lọc
     * @param organizationId Tổ chức cần lọc
     * @param fromDate    Thời điểm bắt đầu phát sinh cảnh báo
     * @param toDate      Thời điểm kết thúc phát sinh cảnh báo
     * @param unitIds     Danh sách địa bàn thu hẹp
     * @return Mảng byte của file Excel .xlsx
     */
    byte[] exportAlertLots(
            CustomUserDetails currentUser,
            LotAlertType alertType,
            UUID organizationId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            List<UUID> unitIds);
}
