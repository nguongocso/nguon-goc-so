package vn.nguongocso.organization.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.response.AssignedAreaResponse;
import vn.nguongocso.organization.service.AreaAssignmentService;

/**
 * API người dùng hiện tại tự xem địa bàn đã được gán (VT-05).
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeAreaController {

    private final AreaAssignmentService areaAssignmentService;

    /**
     * Địa bàn đã gán của người dùng hiện tại.
     *
     * @return danh sách địa bàn đã gán
     */
    @GetMapping("/areas")
    public ResponseEntity<ApiResult<List<AssignedAreaResponse>>> getMyAreas() {
        return ResponseEntity.ok(ApiResult.success(areaAssignmentService.getMyAreas(null)));
    }
}
