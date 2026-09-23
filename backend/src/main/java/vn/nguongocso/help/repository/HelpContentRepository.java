package vn.nguongocso.help.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.help.entity.HelpContent;

/**
 * Repository quản lý nội dung hướng dẫn sử dụng.
 */
@Repository
public interface HelpContentRepository extends JpaRepository<HelpContent, UUID> {
    /*
    /** Tìm nội dung hướng dẫn theo màn hình và mã vai trò (sắp xếp theo số thứ tự).
     */
    List<HelpContent> findByScreenKeyAndRoleCodeOrderBySortOrderAsc(String screenKey, String roleCode);

    /*
     * Tìm nội dung hướng dẫn theo màn hình (sắp xếp theo số thứ tự).
     */
    List<HelpContent> findByScreenKeyOrderBySortOrderAsc(String screenKey);
}