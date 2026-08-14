package vn.nguongocso.event.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.request.StorageConditionRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.ChainVerificationResponse;
import vn.nguongocso.event.dto.response.ScanLookupResponse;
import vn.nguongocso.event.dto.response.StorageConditionResponse;
import vn.nguongocso.event.entity.ChainEvent;

import java.util.List;
import java.util.UUID;

/**
 * Service interface cho nghiệp vụ sự kiện chuỗi cung ứng.
 *
 * Team WEB 1
 */
public interface ChainEventService {

	/**
	 * Ghi nhận sự kiện thu hoạch.
	 */
	ChainEventResponse recordHarvestEvent(RecordHarvestEventRequest request, CustomUserDetails currentUser);

	/**
	 * Ghi nhận sự kiện đóng gói.
	 */
	ChainEventResponse recordPackagingEvent(RecordPackagingEventRequest request, CustomUserDetails currentUser);

	/**
	 * Ghi nhận sự kiện vận chuyển.
	 */
	ChainEventResponse recordTransportEvent(RecordTransportEventRequest request, CustomUserDetails currentUser);

	/**
	 * Sửa đổi sự kiện đóng gói.
	 */
	ChainEventResponse correctPackagingEvent(UUID originalEventId, CorrectPackagingEventRequest request,
			CustomUserDetails currentUser);

	/**
	 * Lấy dòng thời gian các sự kiện của một lô hàng.
	 */
	List<ChainEventResponse> getShipmentTimeline(UUID shipmentId);

	/**
	 * Ghi sự kiện từ thiết bị di động.
	 */
	ChainEventResponse recordMobileEvent(RecordMobileEventRequest request, CustomUserDetails currentUser);

	/**
	 * Tra cứu thông tin qua mã quét.
	 */
	ScanLookupResponse scanLookup(String codeValue, CustomUserDetails currentUser);

	/**
	 * Ghi nhận mốc điều kiện bảo quản khi vận chuyển.
	 */
	StorageConditionResponse recordStorageCondition(StorageConditionRequest request, CustomUserDetails currentUser);

	/**
	 * Kiểm chứng tính toàn vẹn dòng sự kiện của một lô hàng.
	 */
	ChainVerificationResponse verifyChainIntegrity(UUID shipmentId, CustomUserDetails currentUser);

	/**
	 * Lưu ChainEvent và tự động tính chuỗi băm liên kết với sự kiện trước đó
	 * trong cùng Shipment. Đây là điểm tập trung để mọi loại sự kiện được gắn
	 * hash đúng theo QTN-19.
	 */
	ChainEvent saveWithChainHash(ChainEvent event);
}
