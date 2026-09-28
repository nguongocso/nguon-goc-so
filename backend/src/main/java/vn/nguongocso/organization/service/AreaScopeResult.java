package vn.nguongocso.organization.service;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

/** Kết quả phân tích phạm vi địa bàn của người gọi báo cáo. */
@Getter
public final class AreaScopeResult {
    /** Chế độ phạm vi địa bàn. */
    public enum Mode {
        /** Xem toàn bộ tổ chức. */
        ALL,

        /** Chỉ xem danh sách tổ chức cho phép. */
        FILTERED,

        /** Không có phạm vi nào (dữ liệu rỗng hoàn toàn). */
        EMPTY_SCOPE
    }

    private static final AreaScopeResult ALL_INSTANCE = new AreaScopeResult(Mode.ALL, Set.of());

    private static final AreaScopeResult EMPTY_INSTANCE = new AreaScopeResult(Mode.EMPTY_SCOPE, Set.of());

    private final Mode mode;

    /** Tập ID tổ chức được phép xem khi mode == FILTERED. */
    private final Set<UUID> organizationIds;

    private AreaScopeResult(Mode mode, Set<UUID> organizationIds) {
        this.mode = mode;
        this.organizationIds = Set.copyOf(organizationIds);
    }

    public static AreaScopeResult all() {
        return ALL_INSTANCE;
    }

    public static AreaScopeResult emptyScope() {
        return EMPTY_INSTANCE;
    }

    public static AreaScopeResult of(Set<UUID> organizationIds) {
        return new AreaScopeResult(Mode.FILTERED, organizationIds);
    }

    public boolean isAll() {
        return mode == Mode.ALL;
    }

    public boolean isEmptyScope() {
        return mode == Mode.EMPTY_SCOPE;
    }

    public boolean isFiltered() {
        return mode == Mode.FILTERED;
    }
}
