package vn.nguongocso.organization.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.UserAreaAssignment;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Kiểm thử tích hợp tầng dữ liệu NCL-740: danh mục đơn vị hành chính
 * ({@link AdministrativeUnit}), bảng gán địa bàn cho tài khoản
 * ({@link UserAreaAssignment}) và mapping địa bàn trên tổ chức.
 *
 * <p>
 * Sử dụng {@code @SpringBootTest} + profile {@code test} + {@code @Transactional}
 * theo đúng quy ước hạ tầng kiểm thử hiện có (H2, Flyway tắt, ddl-auto=create-drop).
 * Fixture tự tạo trong test, không phụ thuộc seed quốc gia V36.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAreaAssignmentRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AdministrativeUnitRepository administrativeUnitRepository;

    @Autowired
    private UserAreaAssignmentRepository userAreaAssignmentRepository;

    private User canBoNganh;
    private User nguoiThaoTac;
    private Organization organization;
    private AdministrativeUnit tinhNinhBinh;
    private AdministrativeUnit xaHoaLau;

    @BeforeEach
    void setUp() {
        // Tài khoản cán bộ quản lý ngành (VT-05) được gán địa bàn.
        canBoNganh = userRepository.save(User.builder()
                .userName("vt05-ncl740")
                .passwordHash("{noop}mat-khau")
                .fullName("Nguyễn Văn Cán Bộ")
                .email("vt05-ncl740@test.local")
                .status(UserStatus.ACTIVE)
                .build());

        // Tài khoản VT-01 thao tác gán (assigned_by).
        nguoiThaoTac = userRepository.save(User.builder()
                .userName("vt01-ncl740")
                .passwordHash("{noop}mat-khau")
                .fullName("Trần Văn Quản Trị")
                .email("vt01-ncl740@test.local")
                .status(UserStatus.ACTIVE)
                .build());

        // Cấp tỉnh (gốc) và cấp xã con, liên kết denormalize province_id.
        tinhNinhBinh = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("36")
                .name("Ninh Bình")
                .level(AdministrativeUnitLevel.PROVINCE)
                .active(true)
                .build());
        xaHoaLau = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("04098")
                .name("Hoa Lư")
                .level(AdministrativeUnitLevel.COMMUNE)
                .parent(tinhNinhBinh)
                .province(tinhNinhBinh)
                .active(true)
                .build());

        organization = organizationRepository.save(Organization.builder()
                .name("HTX Nông Sạch Hoa Lư")
                .code("HTX-NCL-740")
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .address("Xã Hoa Lư, Ninh Bình")
                .build());
    }

    private UserAreaAssignment assignment(User user, AdministrativeUnit unit) {
        return assignment(user, unit, null);
    }

    private UserAreaAssignment assignment(User user, AdministrativeUnit unit,
            java.time.LocalDateTime assignedAt) {
        UserAreaAssignment row = UserAreaAssignment.builder()
                .user(user)
                .unit(unit)
                .assignedBy(nguoiThaoTac)
                .build();
        if (assignedAt != null) {
            // Gán mốc thời gian tường minh để thứ tự sắp xếp DESC xác định.
            row.setAssignedAt(assignedAt);
        }
        return row;
    }

    @Test
    void saveAndFindByUser_returnsAllAssignmentsOrderedByAssignedAtDesc() {
        userAreaAssignmentRepository.saveAndFlush(
                assignment(canBoNganh, tinhNinhBinh, java.time.LocalDateTime.now().minusMinutes(5)));
        userAreaAssignmentRepository.saveAndFlush(assignment(canBoNganh, xaHoaLau));

        List<UserAreaAssignment> rows = userAreaAssignmentRepository
                .findAllByUser_UserIdOrderByAssignedAtDesc(canBoNganh.getUserId());

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getUser().getUserId()).isEqualTo(canBoNganh.getUserId());
            assertThat(row.getAssignedBy().getUserId()).isEqualTo(nguoiThaoTac.getUserId());
            assertThat(row.getAssignedAt()).isNotNull();
        });
        // Đơn vị mới gán sau có assignedAt muộn hơn -> đứng trước khi sắp xếp DESC.
        assertThat(rows.get(0).getUnit().getCode()).isEqualTo("04098");
        assertThat(rows.get(1).getUnit().getCode()).isEqualTo("36");
    }

    @Test
    void duplicateUserAndUnit_throwsDataIntegrityViolation() {
        userAreaAssignmentRepository.saveAndFlush(assignment(canBoNganh, xaHoaLau));

        // UNIQUE (user_id, unit_id) chặn gán trùng ngay ở tầng dữ liệu.
        assertThrows(DataIntegrityViolationException.class,
                () -> userAreaAssignmentRepository.saveAndFlush(assignment(canBoNganh, xaHoaLau)));
    }

    @Test
    void deleteAssignment_removesRow() {
        UserAreaAssignment saved = userAreaAssignmentRepository.saveAndFlush(
                assignment(canBoNganh, xaHoaLau));

        userAreaAssignmentRepository.delete(saved);
        userAreaAssignmentRepository.flush();

        assertThat(userAreaAssignmentRepository
                .findAllByUser_UserIdOrderByAssignedAtDesc(canBoNganh.getUserId())).isEmpty();
        assertThat(userAreaAssignmentRepository.existsByUser_UserIdAndUnit_Id(
                canBoNganh.getUserId(), xaHoaLau.getId())).isFalse();
    }

    @Test
    void organizationProvinceMapping_persists() {
        organization.setProvince(tinhNinhBinh);
        organization.setCommune(xaHoaLau);
        organizationRepository.saveAndFlush(organization);

        Organization reloaded = organizationRepository.findById(organization.getOrganizationId()).orElseThrow();
        assertThat(reloaded.getProvince().getId()).isEqualTo(tinhNinhBinh.getId());
        assertThat(reloaded.getCommune().getId()).isEqualTo(xaHoaLau.getId());
    }

    @Test
    void findByCode_resolvesBothLevels() {
        AdministrativeUnit byCommuneCode = administrativeUnitRepository.findByCode("04098").orElseThrow();
        assertThat(byCommuneCode.getName()).isEqualTo("Hoa Lư");
        assertThat(byCommuneCode.getLevel()).isEqualTo(AdministrativeUnitLevel.COMMUNE);

        AdministrativeUnit byProvinceCode = administrativeUnitRepository.findByCode("36").orElseThrow();
        assertThat(byProvinceCode.getName()).isEqualTo("Ninh Bình");
        assertThat(byProvinceCode.getLevel()).isEqualTo(AdministrativeUnitLevel.PROVINCE);

        // Danh mục cấp tỉnh sắp xếp theo tên phục vụ dropdown chọn địa bàn.
        List<AdministrativeUnit> provinces = administrativeUnitRepository
                .findAllByLevelOrderByNameAsc(AdministrativeUnitLevel.PROVINCE);
        assertThat(provinces).extracting(AdministrativeUnit::getCode).containsOnlyOnce("36");

        // Xã con tra cứu theo tỉnh gốc qua cột denormalize province_id.
        List<AdministrativeUnit> communes = administrativeUnitRepository
                .findAllByProvinceIdAndLevelOrderByNameAsc(
                        tinhNinhBinh.getId(), AdministrativeUnitLevel.COMMUNE);
        assertThat(communes).hasSize(1);

        assertThat(administrativeUnitRepository.existsByCode(UUID.randomUUID().toString()))
                .as("Mã không tồn tại").isFalse();
    }
}
