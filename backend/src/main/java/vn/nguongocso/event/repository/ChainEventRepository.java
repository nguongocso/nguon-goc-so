package vn.nguongocso.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;

/**
 * Repository cho thực thể ChainEvent.
 *
 * @author Team WEB 1
 */
@Repository
public interface ChainEventRepository extends JpaRepository<ChainEvent, UUID> {
        /**
         * Lấy danh sách sự kiện của một lô hàng, sắp xếp theo thời gian tăng dần.
         *
         * @param shipmentId ID lô hàng
         * @return danh sách sự kiện
         */
        List<ChainEvent> findByShipment_IdOrderByRecordedAtAsc(UUID shipmentId);

        /**
         * Lấy danh sách các điểm hành trình (có tọa độ) của một lô hàng,
         * sắp xếp theo thời gian tăng dần.
         *
         * @param shipmentId ID lô hàng
         * @return danh sách sự kiện có tọa độ
         */
        @Query("""
                        SELECT ce
                        FROM ChainEvent ce
                        WHERE ce.shipment.id = :shipmentId
                          AND ce.location IS NOT NULL
                          AND ce.isCorrection = false
                        ORDER BY ce.recordedAt ASC
                        """)
        List<ChainEvent> findJourneyPointsByShipmentId(@Param("shipmentId") UUID shipmentId);

        /**
         * Lấy danh sách sự kiện của một lô hàng, sắp xếp theo thời gian tăng dần.
         *
         * @param shipmentId ID lô hàng
         * @return danh sách sự kiện
         */
        List<ChainEvent> findByShipmentIdOrderByRecordedAtAsc(UUID shipmentId);

        /**
         * Lấy danh sách sự kiện không thuộc bất kỳ lô hàng nào, với các loại sự kiện
         * nhất định.
         *
         * @param eventTypes Danh sách loại sự kiện
         * @return danh sách sự kiện
         */
        List<ChainEvent> findByShipmentIsNullAndEventTypeIn(List<ChainEventType> eventTypes);

        /**
         * Lấy sự kiện gần nhất của một lô hàng.
         *
         * Phục vụ chức năng quét mã để xác định loại sự kiện
         * hợp lệ tiếp theo khi mở biểu mẫu ghi sự kiện.
         *
         * @param shipmentId ID lô hàng
         * @return sự kiện mới nhất nếu tồn tại
         */
        Optional<ChainEvent> findTopByShipmentIdOrderByRecordedAtDesc(UUID shipmentId);

        /**
         * Lấy sự kiện được ghi gần nhất (createdAt) của một lô hàng.
         *
         * Dùng cho NCL-08 hash chain: thứ tự chuỗi mật mã phải dựa trên
         * thứ tự bất biến mà máy chủ sinh ra (createdAt), KHÔNG dùng recordedAt
         * (client cung cấp). Truy vấn này chỉ lấy 1 bản ghi, tránh tải toàn bộ.
         *
         * @param shipmentId ID lô hàng
         * @return sự kiện được ghi gần nhất nếu tồn tại
         */
        Optional<ChainEvent> findTopByShipmentIdOrderByCreatedAtDesc(UUID shipmentId);

        /**
         * Xóa tất cả sự kiện của một lô hàng.
         *
         * @param id ID lô hàng
         */
        void deleteByShipmentId(UUID id);

        /**
         * Đếm số lượng event theo loại cho từng shipment.
         *
         * @param shipmentIds   danh sách ID lô hàng
         * @param requiredTypes danh sách loại sự kiện
         * @return danh sách kết quả đếm
         */
        @Query("SELECT ce.shipment.id, ce.eventType, COUNT(ce) " +
                "FROM ChainEvent ce " +
                "WHERE ce.shipment.id IN :shipmentIds " +
                "AND ce.eventType IN :requiredTypes " +
                "AND ce.isCorrection = false " +
                "GROUP BY ce.shipment.id, ce.eventType")
        List<Object[]> countEventsByShipmentAndTypes(@Param("shipmentIds") List<UUID> shipmentIds,
                                                     @Param("requiredTypes") List<ChainEventType> requiredTypes);

        /**
         * Lấy danh sách sự kiện (không phải đính chính) của nhiều lô hàng,
         * sắp xếp theo thời gian tăng dần.
         */
        @Query("SELECT ce FROM ChainEvent ce " +
                "WHERE ce.shipment.id IN :shipmentIds " +
                "AND ce.isCorrection = false " +
                "ORDER BY ce.recordedAt ASC")
        List<ChainEvent> findByShipmentIdInOrderByRecordedAtAsc(@Param("shipmentIds") List<UUID> shipmentIds);

        /**
         * Lấy danh sách sự kiện WAREHOUSE_RECEIPT của một tổ chức, phân trang.
         */
        Page<ChainEvent> findByEventTypeAndRecordedBy_UserIdOrderByRecordedAtDesc(
                ChainEventType eventType, UUID userId, Pageable pageable);

        /**
         * Tìm một sự kiện WAREHOUSE_RECEIPT theo ID và loại sự kiện.
         */
        Optional<ChainEvent> findByIdAndEventType(UUID id, ChainEventType eventType);

        /**
         * Lấy danh sách ID người dùng đã ghi sự kiện thu mua (PROCUREMENT)
         * cho các lô hàng được chỉ định.
         *
         * <p>Dùng cho NCL-08-CN-008 để xác định doanh nghiệp thu mua (người mua)
         * liên quan đến một lô sản xuất để gửi thông báo thu hồi.</p>
         *
         * @param shipmentIds danh sách ID lô hàng
         * @return danh sách ID người dùng đã ghi nhận thu mua (không trùng lặp)
         */
        @Query("SELECT DISTINCT ce.recordedBy.id FROM ChainEvent ce " +
                "WHERE ce.shipment.id IN :shipmentIds " +
                "AND ce.eventType = vn.nguongocso.event.enums.ChainEventType.PROCUREMENT " +
                "AND ce.isCorrection = false")
        List<UUID> findDistinctProcurementRecorderIdsByShipmentIds(@Param("shipmentIds") List<UUID> shipmentIds);

        /**
         * Kiểm tra sự tồn tại của sự kiện theo lotId với 2 trường hợp:
         * 1) event gắn trực tiếp vào shipment của lot
         * 2) event chưa gắn shipment nhưng lưu productionLotId trong eventData
         *    (ví dụ HARVEST / PACKAGING do thiết kế hệ thống cũ).
         */
        @Query("""
                            SELECT COUNT(ce) > 0
                            FROM ChainEvent ce
                            LEFT JOIN ce.shipment s
                            WHERE ce.eventType = :eventType
                              AND ce.isCorrection = false
                              AND (
                                  (s.productionLot.id = :productionLotId)
                                  OR (
                                      ce.shipment IS NULL
                                      AND ce.eventData IS NOT NULL
                                      AND FUNCTION('JSON_UNQUOTE',
                                            FUNCTION('JSON_EXTRACT', ce.eventData, '$.productionLotId'))
                                          = :productionLotIdText
                                  )
                              )
                        """)
        boolean existsByProductionLotIdOrUnassignedEventDataAndEventType(
                        @Param("productionLotId") UUID productionLotId,
                        @Param("productionLotIdText") String productionLotIdText,
                        @Param("eventType") ChainEventType eventType);

        /**
         * Kiểm tra sự tồn tại của sự kiện thu mua (PROCUREMENT, không phải đính chính)
         * của một lô hàng.
         *
         * @param shipmentId ID lô hàng
         * @param eventType  loại sự kiện cần kiểm tra
         * @return true nếu tồn tại ít nhất một sự kiện hợp lệ
         */
        @Query("SELECT COUNT(ce) > 0 FROM ChainEvent ce " +
                "WHERE ce.shipment.id = :shipmentId " +
                "AND ce.eventType = :eventType " +
                "AND ce.isCorrection = false")
        boolean existsByShipmentIdAndEventType(
                @Param("shipmentId") UUID shipmentId,
                @Param("eventType") ChainEventType eventType);
}