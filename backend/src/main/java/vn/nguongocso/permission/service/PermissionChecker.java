package vn.nguongocso.permission.service;

import java.util.List;

/**
 * Service kiểm tra quyền của người dùng hiện tại.
 */
public interface PermissionChecker {
    /**
     * Kiểm tra người dùng hiện tại có quyền thực hiện action
     * trên resource hay không.
     *
     * @param resource tên module
     * @param action   CREATE / READ / UPDATE / DELETE ...
     */
    void check(String resource, String action);

    /**
     * Lấy danh sách tất cả các permission code (dạng resource:action)
     * đang có hiệu lực của người dùng hiện tại.
     *
     * @return danh sách permission codes
     */
    List<String> getPermissionsForCurrentUser();
}