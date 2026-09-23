package vn.nguongocso.organization.service;

import java.util.List;

import vn.nguongocso.organization.dto.response.AdministrativeUnitNode;

/** Dịch vụ danh mục đơn vị hành chính dùng chung. */
public interface AdministrativeUnitService {
    /** Dựng cây đơn vị hành chính 2 cấp (tỉnh chứa xã/phường). */
    List<AdministrativeUnitNode> getUnitTree();
}
