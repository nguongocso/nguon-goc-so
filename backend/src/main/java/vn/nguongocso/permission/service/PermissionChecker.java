package vn.nguongocso.permission.service;

import java.util.List;

/** Service kiểm tra quyền của người dùng hiện tại. */
public interface PermissionChecker {
    /** Kiểm tra người dùng hiện tại có quyền thực hiện action trên resource hay không. */
    void check(String resource, String action);

    /** Lấy danh sách tất cả các permission code đang có hiệu lực của người dùng hiện tại. */
    List<String> getPermissionsForCurrentUser();
}