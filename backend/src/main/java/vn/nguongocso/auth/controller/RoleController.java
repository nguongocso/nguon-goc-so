package vn.nguongocso.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.repository.RoleRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
/** Cung cấp danh sách vai trò trong hệ thống. */
public class RoleController {
    private final RoleRepository roleRepository;

    /** Lấy toàn bộ vai trò. */
    @GetMapping
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
