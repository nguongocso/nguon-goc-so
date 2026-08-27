import type { AdministrativeUnitNode } from '@/types/administrativeUnit';

// Dữ liệu giả lập cây đơn vị hành chính 2 cấp (tỉnh → xã/phường) theo mô hình sáp nhập 2025.
// id là UUID cố định để test và seed gán địa bàn ổn định giữa các lần chạy.
export const MOCK_ADMIN_UNITS: AdministrativeUnitNode[] = [
  {
    id: 'a1000000-0000-0000-0000-000000000001',
    code: '01',
    name: 'Hà Nội',
    level: 'PROVINCE',
    children: [
      {
        id: 'a1000000-0000-0000-0000-000000010001',
        code: '00001',
        name: 'Phường Hoàn Kiếm',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a1000000-0000-0000-0000-000000010002',
        code: '00002',
        name: 'Phường Ba Đình',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a1000000-0000-0000-0000-000000010003',
        code: '00004',
        name: 'Phường Tây Hồ',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a1000000-0000-0000-0000-000000010004',
        code: '00007',
        name: 'Xã Sóc Sơn',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a1000000-0000-0000-0000-000000010005',
        code: '00009',
        name: 'Phường Hà Đông',
        level: 'COMMUNE',
        children: [],
      },
    ],
  },
  {
    id: 'a2000000-0000-0000-0000-000000000002',
    code: '25',
    name: 'Phú Thọ',
    level: 'PROVINCE',
    children: [
      {
        id: 'a2000000-0000-0000-0000-000000020001',
        code: '00198',
        name: 'Phường Việt Trì',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a2000000-0000-0000-0000-000000020002',
        code: '00205',
        name: 'Xã Thanh Sơn',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a2000000-0000-0000-0000-000000020003',
        code: '00211',
        name: 'Xã Tân Sơn',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a2000000-0000-0000-0000-000000020004',
        code: '00218',
        name: 'Xã Đoan Hùng',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a2000000-0000-0000-0000-000000020005',
        code: '00224',
        name: 'Xã Phù Ninh',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a2000000-0000-0000-0000-000000020006',
        code: '00231',
        name: 'Xã Lâm Thao',
        level: 'COMMUNE',
        children: [],
      },
    ],
  },
  {
    id: 'a3000000-0000-0000-0000-000000000003',
    code: '11',
    name: 'Điện Biên',
    level: 'PROVINCE',
    children: [
      {
        id: 'a3000000-0000-0000-0000-000000030001',
        code: '00114',
        name: 'Phường Mường Thanh',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a3000000-0000-0000-0000-000000030002',
        code: '00116',
        name: 'Phường Thanh Minh',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a3000000-0000-0000-0000-000000030003',
        code: '00125',
        name: 'Xã Mường Phăng',
        level: 'COMMUNE',
        children: [],
      },
      {
        id: 'a3000000-0000-0000-0000-000000030004',
        code: '00137',
        name: 'Xã Tủa Chùa',
        level: 'COMMUNE',
        children: [],
      },
    ],
  },
];

/** Map phẳng id → node (gồm cả node con), tiện tra cứu tên/cấp. */
export function flattenAdminUnits(
  nodes: AdministrativeUnitNode[],
  map: Map<string, AdministrativeUnitNode> = new Map(),
): Map<string, AdministrativeUnitNode> {
  for (const node of nodes) {
    map.set(node.id, node);
    flattenAdminUnits(node.children, map);
  }
  return map;
}
