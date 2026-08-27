package vn.nguongocso.organization.service;

import java.util.List;

import vn.nguongocso.organization.dto.response.AdministrativeUnitNode;

/**
 * Dịch vụ danh mục đơn vị hành chính dùng chung.
 */
public interface AdministrativeUnitService {

	/**
	 * Dựng cây đơn vị hành chính 2 cấp: tỉnh/thành ở mức gốc, xã/phường lồng
	 * trong children; cả hai mức sắp xếp theo tên.
	 *
	 * @return danh sách node cấp tỉnh
	 */
	List<AdministrativeUnitNode> getUnitTree();
}
