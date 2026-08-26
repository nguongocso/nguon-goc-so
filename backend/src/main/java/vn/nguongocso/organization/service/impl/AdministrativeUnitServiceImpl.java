package vn.nguongocso.organization.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.organization.dto.response.AdministrativeUnitNode;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.repository.AdministrativeUnitRepository;
import vn.nguongocso.organization.service.AdministrativeUnitService;

/**
 * Dựng cây danh mục đơn vị hành chính từ dữ liệu phẳng (~3.3k dòng, tải toàn
 * bộ rồi ghép trong bộ nhớ).
 */
@Service
@RequiredArgsConstructor
public class AdministrativeUnitServiceImpl implements AdministrativeUnitService {

	private final AdministrativeUnitRepository administrativeUnitRepository;

	@Override
	@Transactional(readOnly = true)
	public List<AdministrativeUnitNode> getUnitTree() {
		List<AdministrativeUnit> provinces = administrativeUnitRepository
				.findAllByLevelAndActiveTrueOrderByNameAsc(AdministrativeUnitLevel.PROVINCE);
		List<AdministrativeUnit> communes = administrativeUnitRepository
				.findAllByLevelAndActiveTrueOrderByNameAsc(AdministrativeUnitLevel.COMMUNE);

		Map<UUID, List<AdministrativeUnitNode>> childrenByProvince = new HashMap<>();
		for (AdministrativeUnit commune : communes) {
			UUID provinceId = commune.getProvince() != null ? commune.getProvince().getId() : null;
			if (provinceId == null && commune.getParent() != null) {
				provinceId = commune.getParent().getId();
			}
			if (provinceId == null) {
				continue;
			}
			childrenByProvince
					.computeIfAbsent(provinceId, key -> new ArrayList<>())
					.add(toNode(commune, List.of()));
		}

		List<AdministrativeUnitNode> tree = new ArrayList<>(provinces.size());
		for (AdministrativeUnit province : provinces) {
			tree.add(toNode(province,
					childrenByProvince.getOrDefault(province.getId(), List.of())));
		}
		return tree;
	}

	private AdministrativeUnitNode toNode(AdministrativeUnit unit, List<AdministrativeUnitNode> children) {
		return AdministrativeUnitNode.builder()
				.id(unit.getId())
				.code(unit.getCode())
				.name(unit.getName())
				.level(unit.getLevel().name())
				.children(children)
				.build();
	}
}
