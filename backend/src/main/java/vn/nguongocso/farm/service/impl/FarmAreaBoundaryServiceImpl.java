package vn.nguongocso.farm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import net.sf.geographiclib.PolygonResult;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.config.FarmAreaBoundaryProperties;
import vn.nguongocso.farm.dto.request.LatLngDto;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaBoundaryRequest;
import vn.nguongocso.farm.dto.response.FarmAreaBoundaryResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.service.FarmAreaBoundaryService;

/**
 * Triển khai nghiệp vụ ranh giới vùng trồng trên hệ tọa độ WGS84.
*/
@Service
@RequiredArgsConstructor
public class FarmAreaBoundaryServiceImpl implements FarmAreaBoundaryService {
    private static final String MANAGER_ROLE = "VT-02";
    private static final Set<String> READ_ROLES = Set.of("VT-01", MANAGER_ROLE, "VT-03");

    private static final int SRID_WGS84 = 4326;
    private static final int AREA_SCALE = 4;
    private static final int DEVIATION_CALCULATION_SCALE = 10;
    private static final int DEVIATION_RESPONSE_SCALE = 2;
    private static final int MAX_BOUNDARY_POINTS = 500;

    private final FarmAreaRepository farmAreaRepository;
    private final GeometryFactory geometryFactory;
    private final FarmAreaBoundaryProperties properties;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

    /** Lấy ranh giới vùng trồng theo ID. */
    @Override
    @Transactional(readOnly = true)
    public FarmAreaBoundaryResponse getBoundary(UUID farmAreaId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        ensureReadableRole(currentUser);
        FarmArea farmArea = getOwnedFarmArea(farmAreaId, currentUser);
        return toResponse(farmArea);
    }

    /** Cập nhật ranh giới và diện tích tính toán của vùng trồng. */
    @Override
    @Transactional
    public FarmAreaBoundaryResponse updateBoundary(UUID farmAreaId, UpdateFarmAreaBoundaryRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        ensureManagerRole(currentUser);
        FarmArea farmArea = getOwnedFarmAreaForUpdate(farmAreaId, currentUser);

        Polygon polygon = validateAndCreatePolygon(request != null ? request.getPoints() : null);
        BigDecimal preciseCalculatedArea = calculateGeodesicAreaHa(request.getPoints());
        if (preciseCalculatedArea.signum() <= 0) {
            throw boundaryError("INVALID_BOUNDARY_POINTS",
                    "Ranh giới vùng trồng phải tạo được polygon có diện tích lớn hơn 0");
        }

        BigDecimal storedCalculatedArea = preciseCalculatedArea.setScale(AREA_SCALE, RoundingMode.HALF_UP);
        BigDecimal preciseDeviation = calculateDeviationPercentage(preciseCalculatedArea, farmArea.getArea());
        BigDecimal responseDeviation = preciseDeviation.setScale(DEVIATION_RESPONSE_SCALE, RoundingMode.HALF_UP);
        BigDecimal threshold = properties.getBoundaryDeviationThresholdPercent();

        if (preciseDeviation.compareTo(threshold) > 0 && !Boolean.TRUE.equals(request.getConfirmed())) {
            throw confirmationRequired(farmArea.getArea(), storedCalculatedArea, responseDeviation, threshold);
        }

        String beforeValue = serializeSnapshot(farmArea.getBoundary(), farmArea.getCalculatedArea());
        String afterValue = serializeSnapshot(polygon, storedCalculatedArea);
        LocalDateTime updatedAt = LocalDateTime.now();

        farmArea.setBoundary(polygon);
        farmArea.setCalculatedArea(storedCalculatedArea);
        farmArea.setBoundaryUpdatedAt(updatedAt);
        farmAreaRepository.save(farmArea);

        saveActivityLog(currentUser, farmArea, beforeValue, afterValue);
        return toResponse(farmArea, responseDeviation);
    }

    /** Lấy vùng trồng thuộc tổ chức hiện tại theo ID. */
    private FarmArea getOwnedFarmArea(UUID farmAreaId, CustomUserDetails currentUser) {
        FarmArea farmArea = farmAreaRepository.findById(farmAreaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy vùng trồng",
                        Map.of("code", "FARM_AREA_NOT_FOUND")));

        ensureOwnedOrganization(farmArea, currentUser);
        return farmArea;
    }

    /** Lấy vùng trồng thuộc tổ chức hiện tại theo ID, khóa bi quan để cập nhật. */
    private FarmArea getOwnedFarmAreaForUpdate(UUID farmAreaId, CustomUserDetails currentUser) {
        FarmArea farmArea = farmAreaRepository.findByIdForBoundaryUpdate(farmAreaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy vùng trồng",
                        Map.of("code", "FARM_AREA_NOT_FOUND")));
        ensureOwnedOrganization(farmArea, currentUser);
        return farmArea;
    }

    /** Kiểm tra vùng trồng thuộc cùng tổ chức với người dùng. */
    private void ensureOwnedOrganization(FarmArea farmArea, CustomUserDetails currentUser) {
        UUID ownerOrganizationId = farmArea.getOrganization().getOrganizationId();
        if (!Objects.equals(ownerOrganizationId, currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền truy cập vùng trồng của tổ chức khác",
                    Map.of("code", "FORBIDDEN"));
        }
    }

    /** Kiểm tra người dùng có quyền đọc ranh giới (VT-01, VT-02, VT-03). */
    private void ensureReadableRole(CustomUserDetails currentUser) {
        if (!READ_ROLES.contains(currentUser.getRoleCode())) {
            throw forbiddenRole();
        }
    }

    /** Kiểm tra người dùng có vai trò Quản lý hợp tác xã (VT-02). */
    private void ensureManagerRole(CustomUserDetails currentUser) {
        if (!MANAGER_ROLE.equals(currentUser.getRoleCode())) {
            throw forbiddenRole();
        }
    }

    /** Tạo lỗi từ chối quyền khi vai trò không phù hợp. */
    private BusinessException forbiddenRole() {
        return new BusinessException(HttpStatus.FORBIDDEN,
                "Chỉ Quản lý hợp tác xã (VT-02) mới được cập nhật ranh giới vùng trồng",
                Map.of("code", "FORBIDDEN"));
    }

    /** Kiểm tra tính hợp lệ và tạo polygon ranh giới từ danh sách điểm. */
    private Polygon validateAndCreatePolygon(List<LatLngDto> points) {
        if (points == null || points.size() < 3) {
            throw boundaryError("INVALID_BOUNDARY_POINTS",
                    "Ranh giới vùng trồng phải có tối thiểu 3 đỉnh phân biệt");
        }
        if (points.size() > MAX_BOUNDARY_POINTS) {
            throw boundaryError("INVALID_BOUNDARY_POINTS",
                    "Ranh giới vùng trồng chỉ được có tối đa 500 đỉnh");
        }

        Set<CoordinateKey> distinctPoints = new HashSet<>();
        for (int index = 0; index < points.size(); index++) {
            LatLngDto point = points.get(index);
            validateCoordinate(point);
            distinctPoints.add(new CoordinateKey(point.getLatitude(), point.getLongitude()));

            if (index > 0 && samePoint(points.get(index - 1), point)) {
                throw boundaryError("INVALID_BOUNDARY_POINTS", "Hai đỉnh liên tiếp không được trùng nhau");
            }
        }

        if (distinctPoints.size() < 3) {
            throw boundaryError("INVALID_BOUNDARY_POINTS",
                    "Ranh giới vùng trồng phải có tối thiểu 3 đỉnh phân biệt");
        }
        if (samePoint(points.get(0), points.get(points.size() - 1))) {
            throw boundaryError("INVALID_BOUNDARY_POINTS",
                    "Không lặp lại điểm đầu ở cuối danh sách; hệ thống sẽ tự khép kín polygon");
        }

        Coordinate[] coordinates = new Coordinate[points.size() + 1];
        for (int index = 0; index < points.size(); index++) {
            LatLngDto point = points.get(index);
            coordinates[index] = new Coordinate(point.getLongitude(), point.getLatitude());
        }
        coordinates[points.size()] = new Coordinate(coordinates[0]);

        Polygon polygon = geometryFactory.createPolygon(coordinates);
        polygon.setSRID(SRID_WGS84);

        TopologyValidationError validationError = new IsValidOp(polygon).getValidationError();
        if (validationError != null) {
            int errorType = validationError.getErrorType();
            if (errorType == TopologyValidationError.SELF_INTERSECTION
                    || errorType == TopologyValidationError.RING_SELF_INTERSECTION) {
                throw boundaryError("SELF_INTERSECTING_BOUNDARY",
                        "Ranh giới vùng trồng không hợp lệ do các cạnh tự cắt nhau");
            }
            throw boundaryError("INVALID_BOUNDARY_POINTS", "Ranh giới vùng trồng không hợp lệ");
        }
        if (polygon.getArea() <= 0) {
            throw boundaryError("INVALID_BOUNDARY_POINTS",
                    "Ranh giới vùng trồng phải tạo được polygon có diện tích lớn hơn 0");
        }
        return polygon;
    }

    /** Kiểm tra tọa độ đỉnh hợp lệ và trong khoảng cho phép. */
    private void validateCoordinate(LatLngDto point) {
        if (point == null || point.getLatitude() == null || point.getLongitude() == null
                || !Double.isFinite(point.getLatitude()) || !Double.isFinite(point.getLongitude())) {
            throw boundaryError("INVALID_COORDINATES", "Tọa độ đỉnh không được để trống và phải là số hữu hạn");
        }
        if (point.getLatitude() < -90 || point.getLatitude() > 90
                || point.getLongitude() < -180 || point.getLongitude() > 180) {
            throw boundaryError("INVALID_COORDINATES",
                    "Tọa độ đỉnh không hợp lệ (vĩ độ [-90, 90], kinh độ [-180, 180])");
        }
    }

    /** Kiểm tra hai điểm có trùng tọa độ hay không. */
    private boolean samePoint(LatLngDto first, LatLngDto second) {
        return Double.compare(first.getLatitude(), second.getLatitude()) == 0
                && Double.compare(first.getLongitude(), second.getLongitude()) == 0;
    }

    /** Tính diện tích địa lý theo công thức geodesic trên WGS84, đơn vị ha. */
    private BigDecimal calculateGeodesicAreaHa(List<LatLngDto> points) {
        PolygonArea polygonArea = new PolygonArea(Geodesic.WGS84, false);
        for (LatLngDto point : points) {
            polygonArea.AddPoint(point.getLatitude(), point.getLongitude());
        }
        PolygonResult result = polygonArea.Compute();
        return BigDecimal.valueOf(Math.abs(result.area)).movePointLeft(4);
    }

    /** Tính phần trăm chênh lệch giữa diện tích tính toán và diện tích khai báo. */
    private BigDecimal calculateDeviationPercentage(BigDecimal calculatedArea, BigDecimal declaredArea) {
        if (declaredArea == null || declaredArea.signum() <= 0) {
            throw boundaryError("INVALID_DECLARED_AREA", "Diện tích khai báo phải lớn hơn 0");
        }
        return calculatedArea.subtract(declaredArea).abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(declaredArea, DEVIATION_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /** Tạo lỗi yêu cầu xác nhận khi chênh lệch diện tích vượt ngưỡng. */
    private BusinessException confirmationRequired(BigDecimal declaredArea, BigDecimal calculatedArea,
            BigDecimal deviationPercentage, BigDecimal threshold) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("code", "AREA_DEVIATION_CONFIRMATION_REQUIRED");
        details.put("declaredArea", declaredArea);
        details.put("calculatedArea", calculatedArea);
        details.put("deviationPercentage", deviationPercentage);
        details.put("thresholdPercentage", threshold);

        String message = String.format(
                "Diện tích tính từ ranh giới (%s ha) lệch %s%% so với diện tích khai báo (%s ha), "
                        + "vượt ngưỡng cảnh báo %s%%. Vui lòng xác nhận để tiếp tục lưu.",
                calculatedArea.toPlainString(), deviationPercentage.toPlainString(),
                declaredArea.toPlainString(), threshold.stripTrailingZeros().toPlainString());
        return new BusinessException(HttpStatus.CONFLICT, message, details);
    }

    /** Tạo lỗi nghiệp vụ theo mã và thông điệp cho trước. */
    private BusinessException boundaryError(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message, Map.of("code", code));
    }

    /** Tuần tự hóa dữ liệu ranh giới và diện tích để ghi lịch sử. */
    private String serializeSnapshot(Polygon boundary, BigDecimal calculatedArea) {
        if (boundary == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(new BoundaryAuditSnapshot(
                    1, toPoints(boundary), calculatedArea));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tạo dữ liệu lịch sử ranh giới vùng trồng", exception);
        }
    }

    /** Ghi nhật ký hoạt động khi cập nhật ranh giới vùng trồng. */
    private void saveActivityLog(CustomUserDetails currentUser, FarmArea farmArea,
            String beforeValue, String afterValue) {
        activityLogService.logActivity(ActivityLogRequest.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .actorRole(currentUser.getRoleCode())
                .organizationId(currentUser.getOrganizationId())
                .action("UPDATE_FARM_AREA_BOUNDARY")
                .description("Cập nhật ranh giới vùng trồng '" + farmArea.getName() + "'")
                .entityType("FARM_AREA")
                .entityId(farmArea.getId())
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .ipAddress(IpUtils.getClientIp())
                .build());
    }

    /** Chuyển entity vùng trồng sang DTO kèm độ lệch diện tích tính lại. */
    private FarmAreaBoundaryResponse toResponse(FarmArea farmArea) {
        BigDecimal deviation = null;
        if (farmArea.getCalculatedArea() != null) {
            deviation = calculateDeviationPercentage(farmArea.getCalculatedArea(), farmArea.getArea())
                    .setScale(DEVIATION_RESPONSE_SCALE, RoundingMode.HALF_UP);
        }
        return toResponse(farmArea, deviation);
    }

    /** Chuyển entity vùng trồng sang DTO với độ lệch diện tích cho trước. */
    private FarmAreaBoundaryResponse toResponse(FarmArea farmArea, BigDecimal deviation) {
        return FarmAreaBoundaryResponse.builder()
                .id(farmArea.getId())
                .name(farmArea.getName())
                .organizationId(farmArea.getOrganization().getOrganizationId())
                .declaredArea(farmArea.getArea())
                .declaredAreaUnit(AreaUnit.HA)
                .calculatedArea(farmArea.getCalculatedArea())
                .points(toPoints(farmArea.getBoundary()))
                .areaDeviationPercentage(deviation)
                .thresholdPercentage(properties.getBoundaryDeviationThresholdPercent())
                .updatedAt(farmArea.getBoundaryUpdatedAt())
                .build();
    }

    /** Chuyển polygon ranh giới sang danh sách tọa độ. */
    private List<LatLngDto> toPoints(Polygon polygon) {
        if (polygon == null) {
            return List.of();
        }
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        List<LatLngDto> points = new ArrayList<>(Math.max(0, coordinates.length - 1));
        for (int index = 0; index < coordinates.length - 1; index++) {
            Coordinate coordinate = coordinates[index];
            points.add(new LatLngDto(coordinate.getY(), coordinate.getX()));
        }
        return points;
    }

    private record CoordinateKey(double latitude, double longitude) {
    }

    private record BoundaryAuditSnapshot(int schemaVersion, List<LatLngDto> points, BigDecimal calculatedArea) {
    }
}
