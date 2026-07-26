package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.NotificationService;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.QRCodeService;
import vn.nguongocso.trace.service.ShipmentService;
import vn.nguongocso.trace.dto.response.TraceCodeResponse;

/**
 * Service xử lý nghiệp vụ quản lý lô hàng và sinh mã truy xuất.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

	private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);

	private final ShipmentRepository shipmentRepository;
	private final TraceCodeRepository traceCodeRepository;
	private final CodeRangeRepository codeRangeRepository;
	private final ProductionLotRepository productionLotRepository;
	private final QRCodeService qrCodeService;
	private final UserRepository userRepository;

	private final NotificationService notificationService;

	private static final String ORG_MANAGER_ROLE = "VT-02";

	private static final String ORGANIZATION_ACCESS_MESSAGE = "Bạn không thuộc tổ chức của lô sản xuất.";

	private static final String INVALID_LOT_STATUS_MESSAGE = "Chỉ có thể tạo lô hàng từ lô sản xuất đã đóng gói.";

	private static final String CODE_RANGE_NOT_FOUND_MESSAGE = "Tổ chức chưa được cấp dải mã truy xuất.";

	private static final String CODE_RANGE_LIMIT_EXCEEDED_MESSAGE = "Số lượng tem vượt quá hạn mức dải mã còn lại.";

	private static final String PRODUCTION_LOT_NOT_FOUND_MESSAGE = "Không tìm thấy lô sản xuất.";

	/**
	 * Tạo lô hàng và sinh mã truy xuất cho lô sản xuất.
	 *
	 * @param request thông tin tạo lô hàng
	 * @return thông tin lô hàng sau khi tạo
	 * @throws BusinessException nếu không đủ điều kiện tạo lô hàng
	 */
	@Override
	public ShipmentResponse createShipment(CreateShipmentRequest request) {

		log.info("Creating shipment: name={}, productionLotId={}, totalQuantity={}",
				request.getName(), request.getProductionLotId(), request.getTotalQuantity());

		CustomUserDetails currentUser = getCurrentUser();

		validateRole(currentUser, ORG_MANAGER_ROLE, "Bạn không có quyền tạo lô hàng.");

		log.debug("User authenticated: userId={}, organizationId={}, role={}",
				currentUser.getUserId(), currentUser.getOrganizationId(), currentUser.getRoleCode());

		ProductionLot productionLot = findProductionLot(request.getProductionLotId());

		validateOrganization(currentUser, productionLot);

		validateProductionLotStatus(productionLot);

		CodeRange codeRange = findAvailableCodeRange(currentUser);

		validateCodeRangeLimit(codeRange, request.getTotalQuantity());

		Shipment shipment = createShipmentEntity(request, productionLot, currentUser);

		shipment = shipmentRepository.save(shipment);
		log.debug("Shipment saved with id: {}", shipment.getId());

		List<TraceCode> traceCodes = generateTraceCodes(shipment, codeRange, request.getTotalQuantity());
		log.debug("Generated {} trace codes", traceCodes.size());

		traceCodes = traceCodeRepository.saveAll(traceCodes);
		log.debug("Saved {} trace codes to database", traceCodes.size());

		updateCodeRange(codeRange, request.getTotalQuantity());
		codeRangeRepository.save(codeRange);
		log.debug("Updated code range usedCount: {}/{}", codeRange.getUsedCount(), codeRange.getTotalLimit());

			checkAndSendAlert(codeRange);

		shipment.setStatus(ShipmentStatus.CODE_PRINTED);

		String createdByName = currentUser.getFullName();
		ShipmentResponse response = buildShipmentResponse(shipment, traceCodes, createdByName);
		log.info("Shipment created successfully: id={}, traceCodes={}", response.getId(), traceCodes.size());

		return response;
	}

	/**
	 * Kích hoạt tem cho lô hàng và cập nhật trạng thái tem liên kết.
	 *
	 * @param shipmentId id lô hàng cần kích hoạt
	 * @return thông tin lô hàng sau khi kích hoạt
	 * @throws BusinessException nếu không đủ điều kiện kích hoạt tem
	 */
	@Override
	public ShipmentResponse activateShipmentStamps(UUID shipmentId) {
		CustomUserDetails currentUser = getCurrentUser();

		validateRole(currentUser, ORG_MANAGER_ROLE, "Bạn không có quyền kích hoạt tem.");

		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

		if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
			throw new BusinessException("Bạn không có quyền kích hoạt tem của tổ chức khác.");
		}

		ProductionLot productionLot = shipment.getProductionLot();
		if (productionLot == null || productionLot.getStatus() != ProductionLotStatus.PACKAGED) {
			throw new BusinessException(INVALID_LOT_STATUS_MESSAGE);
		}

		if (shipment.getStatus() == ShipmentStatus.ACTIVATED) {
			throw new BusinessException("Tem đã được kích hoạt trước đó.");
		}

		if (shipment.getStatus() != ShipmentStatus.CODE_PRINTED) {
			throw new BusinessException("Lô hàng chưa được cấp hoặc in mã tem.");
		}

		User actor = userRepository.findById(currentUser.getUserId())
				.orElseThrow(() -> new BusinessException("Người dùng không tồn tại."));

		shipment.setStatus(ShipmentStatus.ACTIVATED);
		shipmentRepository.save(shipment);

		List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipmentId);
		LocalDateTime now = LocalDateTime.now();
		for (TraceCode tc : traceCodes) {
			tc.setStatus(TraceCodeStatus.ACTIVE);
			tc.setActivatedAt(now);
			tc.setActivatedBy(actor);
		}
		traceCodeRepository.saveAll(traceCodes);

		String createdByName = null;
		if (shipment.getCreatedBy() != null) {
			createdByName = userRepository.findById(shipment.getCreatedBy().getUserId())
					.map(User::getFullName)
					.orElse(null);
		}

		return buildShipmentResponse(shipment, traceCodes, createdByName);
	}


	/**
	 * Lấy thông tin người dùng đang đăng nhập từ SecurityContext.
	 *
	 * @return thông tin người dùng hiện tại
	 */
	private CustomUserDetails getCurrentUser() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		return (CustomUserDetails) authentication.getPrincipal();
	}

	/**
	 * Kiểm tra người dùng có đúng vai trò được phép thực hiện nghiệp vụ.
	 *
	 * @param currentUser người dùng hiện tại
	 * @param expectedRole mã vai trò yêu cầu
	 * @param message thông báo lỗi nếu không đủ quyền
	 * @throws BusinessException nếu người dùng không có quyền
	 */
	private void validateRole(CustomUserDetails currentUser, String expectedRole, String message) {

		if (!expectedRole.equals(currentUser.getRoleCode())) {
			throw new BusinessException(message);
		}
	}

	/**
	 * Tìm lô sản xuất theo id.
	 *
	 * @param productionLotId id lô sản xuất
	 * @return lô sản xuất
	 * @throws BusinessException nếu không tìm thấy lô sản xuất
	 */
	private ProductionLot findProductionLot(UUID productionLotId) {

		return productionLotRepository.findById(productionLotId)
				.orElseThrow(() -> new BusinessException(PRODUCTION_LOT_NOT_FOUND_MESSAGE));
	}

	/**
	 * Kiểm tra người dùng có quyền thao tác trên lô sản xuất
	 * thuộc tổ chức của mình.
	 *
	 * @param currentUser người dùng hiện tại
	 * @param productionLot lô sản xuất cần kiểm tra
	 * @throws BusinessException nếu khác tổ chức
	 */
	private void validateOrganization(CustomUserDetails currentUser, ProductionLot productionLot) {

		if (!productionLot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {

			throw new BusinessException(ORGANIZATION_ACCESS_MESSAGE);
		}
	}

	/**
	 * Kiểm tra lô sản xuất đã ở trạng thái đóng gói
	 * trước khi tạo lô hàng.
	 *
	 * @param productionLot lô sản xuất
	 * @throws BusinessException nếu trạng thái không hợp lệ
	 */
	private void validateProductionLotStatus(ProductionLot productionLot) {

		if (productionLot.getStatus() != ProductionLotStatus.PACKAGED) {

			throw new BusinessException(INVALID_LOT_STATUS_MESSAGE);
		}
	}

	
	/**
	 * Lấy dải mã truy xuất còn hiệu lực của tổ chức.
	 *
	 * @param currentUser người dùng hiện tại
	 * @return dải mã truy xuất
	 * @throws BusinessException nếu tổ chức chưa được cấp dải mã
	 */
	private CodeRange findAvailableCodeRange(CustomUserDetails currentUser) {

		UUID organizationId = currentUser.getOrganizationId();

		// 1. Load all CodeRanges belonging to the organization
		List<CodeRange> allCodeRanges = codeRangeRepository.findAllByOrganizationOrganizationId(organizationId);

		if (allCodeRanges.isEmpty()) {
			throw new BusinessException(CODE_RANGE_NOT_FOUND_MESSAGE);
		}

		// 2. Find CodeRanges with available capacity (totalLimit > usedCount)
		List<CodeRange> available = allCodeRanges.stream()
				.filter(cr -> cr.getTotalLimit() > cr.getUsedCount())
				.toList();

		if (available.isEmpty()) {
			// All CodeRanges are exhausted
			throw new BusinessException(CODE_RANGE_LIMIT_EXCEEDED_MESSAGE);
		}

		// 3. Select the CodeRange with greatest remaining capacity (deterministic)
		CodeRange candidate = available.stream()
				.max(Comparator.comparingLong(cr -> cr.getTotalLimit() - cr.getUsedCount()))
				.orElseThrow(() -> new BusinessException(CODE_RANGE_LIMIT_EXCEEDED_MESSAGE));

		// 4. Lock the selected CodeRange using PESSIMISTIC_WRITE
		CodeRange lockedCodeRange = codeRangeRepository
				.findByIdAndOrganizationIdForUpdate(candidate.getId(), organizationId)
				.orElseThrow(() -> new BusinessException(CODE_RANGE_NOT_FOUND_MESSAGE));

		// 5. Re-check the actual current capacity after acquiring the lock
		long remaining = lockedCodeRange.getTotalLimit() - lockedCodeRange.getUsedCount();
		if (remaining <= 0) {
			throw new BusinessException(CODE_RANGE_LIMIT_EXCEEDED_MESSAGE);
		}

		return lockedCodeRange;
	}

	/**
	 * Kiểm tra số lượng tem cần sinh có vượt quá
	 * số lượng mã còn lại trong dải mã hay không.
	 *
	 * @param codeRange dải mã truy xuất
	 * @param requiredQuantity số lượng tem cần sinh
	 * @throws BusinessException nếu vượt quá hạn mức
	 */
	private void validateCodeRangeLimit(CodeRange codeRange, long requiredQuantity) {

		long remaining = Math.max(0, codeRange.getTotalLimit() - codeRange.getUsedCount());

		if (requiredQuantity > remaining) {

			throw new BusinessException(CODE_RANGE_LIMIT_EXCEEDED_MESSAGE);
		}
	}

	/**
	 * Khởi tạo đối tượng lô hàng từ yêu cầu tạo lô hàng.
	 *
	 * @param request thông tin tạo lô hàng
	 * @param productionLot lô sản xuất
	 * @param currentUser người dùng tạo
	 * @return đối tượng lô hàng
	 */
	private Shipment createShipmentEntity(CreateShipmentRequest request, ProductionLot productionLot,
			CustomUserDetails currentUser) {

		Shipment shipment = new Shipment();

		shipment.setProductionLot(productionLot);
		shipment.setOrganization(productionLot.getOrganization());

		shipment.setName(request.getName());
		shipment.setTotalQuantity(request.getTotalQuantity());
		shipment.setPackagingInfo(request.getPackagingInfo());

		shipment.setStatus(ShipmentStatus.DRAFT);

		User createdBy = userRepository.findById(currentUser.getUserId())
				.orElseThrow(() -> new BusinessException("Người dùng không tồn tại."));

		shipment.setCreatedBy(createdBy);

		return shipment;
	}

	/**
	 * Sinh danh sách mã truy xuất cho lô hàng.
	 *
	 * @param shipment lô hàng
	 * @param codeRange dải mã truy xuất
	 * @param quantity số lượng mã cần sinh
	 * @return danh sách mã truy xuất
	 */
	private List<TraceCode> generateTraceCodes(Shipment shipment, CodeRange codeRange, long quantity) {

		List<TraceCode> traceCodes = new ArrayList<>();

		long startSequence = codeRange.getUsedCount() + 1;
		
	    UUID organizationId = shipment.getOrganization().getOrganizationId();
	    UUID productionLotId = shipment.getProductionLot().getId();
	    UUID shipmentId = shipment.getId();

		for (long i = 0; i < quantity; i++) {
			
			String codeValue = generateUniqueCode(codeRange.getPrefix(), startSequence + i);
			
			String qrImagePath = qrCodeService.generateQRCode(codeValue, organizationId, productionLotId, shipmentId);

			TraceCode traceCode = new TraceCode();
			
			traceCode.setQrImage(qrImagePath); 

			traceCode.setShipment(shipment);
			
			traceCode.setCodeValue(codeValue);

			traceCode.setStatus(TraceCodeStatus.INACTIVE);

			traceCodes.add(traceCode);
		}

		return traceCodes;
	}

	/**
	 * Sinh giá trị mã truy xuất duy nhất từ tiền tố
	 * và số thứ tự trong dải mã.
	 *
	 * @param prefix tiền tố mã
	 * @param sequence số thứ tự
	 * @return mã truy xuất
	 */
	private String generateUniqueCode(String prefix, long sequence) {

		return prefix + String.format("%08d", sequence);
	}

	private void updateCodeRange(CodeRange codeRange, long quantity) {

		codeRange.setUsedCount(codeRange.getUsedCount() + quantity);
	}

	/**
	 * Xây dựng dữ liệu phản hồi sau khi tạo lô hàng
	 * và sinh mã truy xuất thành công.
	 *
	 * @param shipment lô hàng
	 * @param traceCodes danh sách mã truy xuất
	 * @param createdByName tên người tạo
	 * @return thông tin phản hồiF
	 */
	private ShipmentResponse buildShipmentResponse(Shipment shipment, List<TraceCode> traceCodes,
			String createdByName) {

		return ShipmentResponse.builder().id(shipment.getId()).productionLotId(shipment.getProductionLot().getId())
				.productionLotName(shipment.getProductionLot().getName()).name(shipment.getName())
				.totalQuantity(shipment.getTotalQuantity()).packagingInfo(shipment.getPackagingInfo())
				.status(shipment.getStatus())
				.traceCodes(traceCodes.stream()
						.map(traceCode -> TraceCodeResponse.builder().id(traceCode.getId())
								.codeValue(traceCode.getCodeValue()).qrImage(traceCode.getQrImage())
								.status(traceCode.getStatus()).build())
						.toList())
				.createdByName(createdByName).createdAt(shipment.getCreatedAt()).build();
	}

	@Override
	public List<ShipmentResponse> getShipmentsByOrganization(UUID organizationId) {
		log.info("Fetching shipments for organization: {}", organizationId);
		
		List<Shipment> shipments = shipmentRepository.findByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId);
		
		return shipments.stream()
				.map(shipment -> {
					String createdByName = null;
					if (shipment.getCreatedBy() != null) {
						createdByName = userRepository.findById(shipment.getCreatedBy().getUserId())
								.map(User::getFullName)
								.orElse(null);
					}
					List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipment.getId());
					return buildShipmentResponse(shipment, traceCodes, createdByName);
				})
				.collect(java.util.stream.Collectors.toList());
	}

	private void checkAndSendAlert(CodeRange range) {
		double percent = (double) range.getUsedCount() / range.getTotalLimit() * 100;
		if (percent >= 80 && percent < 100) {
			notificationService.sendAlert(
					"Cảnh báo: Dải mã " + range.getPrefix() + " đã sử dụng " + range.getUsedCount() + "/" + range.getTotalLimit() + " (gần mức hết hạn)"
			);
		} else if (percent >= 100) {
			notificationService.sendAlert(
					"Cảnh báo: Dải mã " + range.getPrefix() + " đã vượt hạn mức " + range.getTotalLimit() + "!"
			);
		}
	}
}
