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

/** Service quản lý danh sách và chi tiết lô có cảnh báo theo địa bàn. */
public interface TerritoryLotAlertService {

 /**
  * Lấy danh sách lô có cảnh báo theo địa bàn của người dùng và các bộ lọc.
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
  * Lấy chi tiết thông tin lô có cảnh báo cùng bằng chứng và dòng sự kiện.
  */
 AlertLotDetailResponse getAlertLotDetail(
   CustomUserDetails currentUser,
   UUID lotId);

 /**
  * Xuất file Excel danh sách lô có cảnh báo theo địa bàn và bộ lọc.
  */
 byte[] exportAlertLots(
   CustomUserDetails currentUser,
   LotAlertType alertType,
   UUID organizationId,
   LocalDateTime fromDate,
   LocalDateTime toDate,
   List<UUID> unitIds);
}
