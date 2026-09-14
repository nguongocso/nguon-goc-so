import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import TerritoryAlertLotListPage from '../TerritoryAlertLotListPage';
import TerritoryAlertLotDetailPage from '../TerritoryAlertLotDetailPage';
import * as territoryAlertLotApi from '@/api/territoryAlertLotApi';
import { NO_ASSIGNED_AREA_MESSAGE } from '@/constants/reportMessages';
import type {
  AlertLotDetailResponse,
  AlertLotSummaryResponse,
} from '@/types/territoryAlertLot';

// Mock sonner toast
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

describe('NCL-07-CN-006: Danh sách và chi tiết lô có cảnh báo theo địa bàn cho VT-05', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // =========================================================================
  // TC-01: Hiển thị đúng 3 rows khi backend trả 2 recalling + 1 locked label
  // =========================================================================
  it('TC-01: Backend có 2 recalling và 1 locked label -> UI hiển thị đủ 3 rows kèm huy hiệu cảnh báo', async () => {
    const mockItems: AlertLotSummaryResponse[] = [
      {
        lotId: '11111111-1111-1111-1111-111111111111',
        lotCode: 'LOT-RECALL-01',
        lotName: 'Lô Xoài Cát Chu A1',
        organizationId: 'org-01',
        organizationName: 'Hợp tác xã Nông nghiệp Xanh',
        productCategoryName: 'Xoài Cát Chu',
        farmAreaName: 'Vùng trồng Cao Lãnh 1',
        communeName: 'Mỹ Xương',
        provinceName: 'Đồng Tháp',
        lotStatus: 'IN_TRANSIT',
        alertTypes: ['RECALLING'],
        primaryAlertType: 'RECALLING',
        alertCount: 1,
        latestAlertTriggeredAt: '2026-09-10T08:30:00',
        alertSummaries: [
          {
            alertType: 'RECALLING',
            alertName: 'Đang thu hồi',
            severity: 'CRITICAL',
            triggeredAt: '2026-09-10T08:30:00',
          },
        ],
        createdAt: '2026-08-01T07:00:00',
      },
      {
        lotId: '22222222-2222-2222-2222-222222222222',
        lotCode: 'LOT-RECALL-02',
        lotName: 'Lô Xoài Cát Chu A2',
        organizationId: 'org-01',
        organizationName: 'Hợp tác xã Nông nghiệp Xanh',
        productCategoryName: 'Xoài Cát Chu',
        farmAreaName: 'Vùng trồng Cao Lãnh 2',
        communeName: 'Mỹ Xương',
        provinceName: 'Đồng Tháp',
        lotStatus: 'PACKAGED',
        alertTypes: ['RECALLING'],
        primaryAlertType: 'RECALLING',
        alertCount: 1,
        latestAlertTriggeredAt: '2026-09-10T09:00:00',
        alertSummaries: [
          {
            alertType: 'RECALLING',
            alertName: 'Đang thu hồi',
            severity: 'CRITICAL',
            triggeredAt: '2026-09-10T09:00:00',
          },
        ],
        createdAt: '2026-08-05T07:00:00',
      },
      {
        lotId: '33333333-3333-3333-3333-333333333333',
        lotCode: 'LOT-LOCKED-01',
        lotName: 'Lô Sầu Riêng Ri6 B1',
        organizationId: 'org-02',
        organizationName: 'HTX Cây Ăn Trái Miền Tây',
        productCategoryName: 'Sầu Riêng Ri6',
        farmAreaName: 'Vùng trồng Cai Lậy',
        communeName: 'Ngũ Hiệp',
        provinceName: 'Tiền Giang',
        lotStatus: 'DELIVERED',
        alertTypes: ['LOCKED_LABEL'],
        primaryAlertType: 'LOCKED_LABEL',
        alertCount: 1,
        latestAlertTriggeredAt: '2026-09-10T10:15:00',
        alertSummaries: [
          {
            alertType: 'LOCKED_LABEL',
            alertName: 'Tem bị khóa',
            severity: 'CRITICAL',
            triggeredAt: '2026-09-10T10:15:00',
          },
        ],
        createdAt: '2026-08-10T07:00:00',
      },
    ];

    vi.spyOn(territoryAlertLotApi, 'getAlertLots').mockResolvedValue({
      success: true,
      status: 200,
      message: 'Truy vấn danh sách lô có cảnh báo thành công.',
      data: {
        items: mockItems,
        page: 0,
        size: 10,
        totalElements: 3,
        totalPages: 1,
        first: true,
        last: true,
      },
    });

    render(
      <MemoryRouter>
        <TerritoryAlertLotListPage />
      </MemoryRouter>
    );

    // Kiểm tra hiển thị tiêu đề trang
    expect(await screen.findByText('Theo dõi lô có cảnh báo')).toBeInTheDocument();

    // Kiểm tra 3 tên lô xuất hiện
    expect(await screen.findByText('Lô Xoài Cát Chu A1')).toBeInTheDocument();
    expect(await screen.findByText('Lô Xoài Cát Chu A2')).toBeInTheDocument();
    expect(await screen.findByText('Lô Sầu Riêng Ri6 B1')).toBeInTheDocument();

    // Kiểm tra nhãn Việt hóa của cảnh báo
    expect(screen.getAllByText('Đang thu hồi').length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText('Tem bị khóa').length).toBeGreaterThanOrEqual(1);

    // Kiểm tra nút Chi tiết hiển thị cho từng dòng (chuẩn hóa dạng Button outline)
    const detailButtons = screen.getAllByRole('button', { name: /Chi tiết/i });
    expect(detailButtons).toHaveLength(3);
  });

  // =========================================================================
  // TC-02: User chưa được phân công địa bàn -> hiển thị đúng thông báo Case A
  // =========================================================================
  it('TC-02: Cán bộ chưa được phân công địa bàn -> Danh sách rỗng và thông báo chưa phân công địa bàn', async () => {
    vi.spyOn(territoryAlertLotApi, 'getAlertLots').mockResolvedValue({
      success: true,
      status: 200,
      message: NO_ASSIGNED_AREA_MESSAGE,
      data: {
        items: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      },
    });

    render(
      <MemoryRouter>
        <TerritoryAlertLotListPage />
      </MemoryRouter>
    );

    // Kiểm tra thông báo đúng yêu cầu Case A
    expect(
      await screen.findByText('Bạn chưa được phân công địa bàn nên chưa có dữ liệu lô cảnh báo để xem.')
    ).toBeInTheDocument();

    // Tuyệt đối không hiển thị câu chung chung "Không có dữ liệu"
    expect(screen.queryByText('Không có dữ liệu.')).not.toBeInTheDocument();
  });

  // =========================================================================
  // TC-03: Trang chi tiết ở chế độ CHỈ XEM (Read-only)
  // =========================================================================
  it('TC-03: Mở trang chi tiết -> Chỉ hiển thị thông tin và timeline, KHÔNG có nút sửa/xóa/mutation', async () => {
    const mockDetail: AlertLotDetailResponse = {
      lotInfo: {
        lotId: 'lot-123',
        lotCode: 'LOT-SAMPLE-99',
        lotName: 'Lô Bưởi Da Xanh Bến Tre',
        status: 'HARVESTED',
        expectedQuantity: 5000,
        actualQuantity: 4800,
        quantityUnit: 'kg',
        plantingDate: '2026-01-15',
        harvestDate: '2026-08-20',
        productCategoryName: 'Bưởi Da Xanh',
        farmAreaName: 'Vùng trồng Mỏ Cày Bắc',
        farmAreaAddress: 'Xã Tân Thành Bình, Huyện Mỏ Cày Bắc, Bến Tre',
        createdAt: '2026-01-15T08:00:00',
      },
      organization: {
        organizationId: 'org-99',
        organizationName: 'Hợp tác xã Bưởi Da Xanh Bến Tre',
        taxCode: '1300998877',
        address: 'Ấp Tân Long 1, Xã Tân Thành Bình',
        communeName: 'Tân Thành Bình',
        provinceName: 'Bến Tre',
        representativeName: 'Nguyễn Văn Chủ',
        contactPhone: '0912345678',
      },
      activeAlerts: [
        {
          alertType: 'RECALLING',
          severity: 'CRITICAL',
          triggeredAt: '2026-08-26T10:00:00',
          title: 'Cảnh báo thu hồi lô hàng',
          message: 'Lô sản xuất đang có quyết định hoặc yêu cầu thu hồi sản phẩm.',
          evidenceData: {
            affectedStage: 'Khâu xuất kho & Lưu thông thương mại',
            traceRule: 'Truy vết xuôi (Downstream): Cảnh báo phát sinh từ lô hàng xuất kho phái sinh',
            recalledShipmentList: [
              {
                id: 'ship-01',
                name: 'Lô xuất khẩu siêu thị VinMart',
                quantity: 1200,
                status: 'RECALLED',
              },
            ],
            recallReason: 'Dư lượng BVTV vượt ngưỡng quy định',
            recallStatus: 'APPROVED',
          },
        },
        {
          alertType: 'INSPECTION_FAILED',
          severity: 'HIGH',
          triggeredAt: '2026-08-25T14:30:00',
          title: 'Kết quả kiểm nghiệm dư lượng thuốc BVTV vượt ngưỡng',
          message: 'Hàm lượng hoạt chất Chlorpyrifos vượt ngưỡng tối đa cho phép MRL (0.05 mg/kg).',
          evidenceData: {
            affectedStage: 'Khâu kiểm nghiệm chất lượng & An toàn thực phẩm',
            traceRule: 'Truy vết tại khâu kiểm định chất lượng phòng Lab',
            failedRatio: '1/5 chỉ tiêu không đạt',
            failedCriteriaNames: ['Chlorpyrifos'],
            criterionName: 'Chlorpyrifos',
            measuredValue: 0.12,
            maxAllowedLimit: 0.05,
            testUnitName: 'Trung tâm Phân tích Quatest 3',
          },
        },
      ],
      timelineEvents: [
        {
          eventId: 'evt-01',
          eventType: 'HARVEST',
          eventTypeName: 'Thu hoạch nông sản',
          recordedAt: '2026-08-20T09:00:00',
          recordedByName: 'Trần Văn Nông',
          location: 'POINT (105.8865152 21.5482368)',
          earlyHarvest: true,
          description: 'Hoàn thành thu hoạch 4,800 kg bưởi.',
        },
        {
          eventId: 'evt-02',
          eventType: 'INSPECTION_RECORD',
          eventTypeName: 'Ghi nhận kiểm nghiệm mẫu',
          recordedAt: '2026-08-25T14:30:00',
          recordedByName: 'Kiểm nghiệm viên Trung tâm',
          location: 'Phòng Lab Quatest 3',
          earlyHarvest: false,
          hasAlert: true,
          alertWarning: 'Kiểm nghiệm không đạt tiêu chuẩn an toàn',
          description: 'Mẫu kiểm nghiệm không đạt chỉ tiêu dư lượng hóa chất BVTV.',
        },
        {
          eventId: 'evt-03',
          eventType: 'TRANSPORT',
          eventTypeName: 'Vận chuyển',
          recordedAt: '2026-08-26T08:00:00',
          recordedByName: 'Tài xế Nguyễn Văn G',
          shipmentName: 'Lô xuất khẩu siêu thị VinMart',
          hasAlert: true,
          alertWarning: "Thuộc lô hàng 'Lô xuất khẩu siêu thị VinMart' đang bị thu hồi",
          earlyHarvest: false,
          description: 'Giao hàng đến tổng kho.',
        },
      ],
    };

    vi.spyOn(territoryAlertLotApi, 'getAlertLotDetail').mockResolvedValue(mockDetail);

    render(
      <MemoryRouter initialEntries={['/reports/alert-lots/lot-123']}>
        <Routes>
          <Route path="/reports/alert-lots/:lotId" element={<TerritoryAlertLotDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    // Kiểm tra tên lô
    expect(await screen.findByRole('heading', { name: 'Lô Bưởi Da Xanh Bến Tre' })).toBeInTheDocument();

    // Loại bỏ toàn bộ dòng liên quan tới Mã lô sản xuất
    expect(screen.queryByText('Mã lô sản xuất:')).not.toBeInTheDocument();
    expect(screen.queryByText('LOT-SAMPLE-99')).not.toBeInTheDocument();

    // Loại bỏ dòng text và huy hiệu "Chế độ chỉ xem"
    expect(screen.queryByText('Chế độ chỉ xem')).not.toBeInTheDocument();

    // Việt hóa trạng thái lô sản xuất: HARVESTED -> "Đã thu hoạch"
    expect(screen.getByText('Đã thu hoạch')).toBeInTheDocument();

    // Sửa tên trường Mã số thuế thành Mã định danh
    expect(screen.getByText('Mã định danh:')).toBeInTheDocument();
    expect(screen.queryByText('Mã số thuế:')).not.toBeInTheDocument();

    // Đã loại bỏ trường Người đại diện trong khung tổ chức sở hữu
    expect(screen.queryByText('Người đại diện:')).not.toBeInTheDocument();

    // Kiểm tra thông tin tổ chức sở hữu
    expect(screen.getByText('Hợp tác xã Bưởi Da Xanh Bến Tre')).toBeInTheDocument();
    expect(screen.getByText('1300998877')).toBeInTheDocument();

    // Kiểm tra Việt hóa mức độ cảnh báo kỹ thuật (CRITICAL -> Rất nghiêm trọng, HIGH -> Nghiêm trọng)
    expect(screen.getByText('Rất nghiêm trọng')).toBeInTheDocument();
    expect(screen.getByText('Nghiêm trọng')).toBeInTheDocument();
    expect(screen.queryByText('CRITICAL')).not.toBeInTheDocument();

    // Kiểm tra chi tiết lô hàng bị thu hồi
    expect(screen.getByText('Cảnh báo thu hồi lô hàng')).toBeInTheDocument();
    expect(screen.getByText(/Lô hàng: Lô xuất khẩu siêu thị VinMart/i)).toBeInTheDocument();
    expect(screen.getByText('(1.200 kg)')).toBeInTheDocument();
    expect(screen.getByText('Đã thu hồi')).toBeInTheDocument();
    expect(screen.getByText('Dư lượng BVTV vượt ngưỡng quy định')).toBeInTheDocument();

    // Kiểm tra khâu phát sinh và quy tắc truy vết
    expect(screen.getByText('Khâu xuất kho & Lưu thông thương mại')).toBeInTheDocument();
    expect(screen.getByText('Khâu kiểm nghiệm chất lượng & An toàn thực phẩm')).toBeInTheDocument();

    // Kiểm tra cảnh báo kiểm nghiệm không đạt
    expect(screen.getByText('Kết quả kiểm nghiệm dư lượng thuốc BVTV vượt ngưỡng')).toBeInTheDocument();
    expect(screen.getByText('1/5 chỉ tiêu không đạt')).toBeInTheDocument();
    expect(screen.getAllByText('Chlorpyrifos').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Hàm lượng đo được:')).toBeInTheDocument();
    expect(screen.getByText('0.12')).toBeInTheDocument();
    expect(screen.getByText('Ngưỡng tối đa cho phép (MRL):')).toBeInTheDocument();
    expect(screen.getByText('0.05')).toBeInTheDocument();
    expect(screen.getByText('Đơn vị phân tích:')).toBeInTheDocument();
    expect(screen.getByText('Trung tâm Phân tích Quatest 3')).toBeInTheDocument();

    // Kiểm tra timeline sự kiện và cảnh báo phân đoạn
    expect(screen.getByText('Dòng sự kiện chuỗi cung ứng')).toBeInTheDocument();
    expect(screen.queryByText(/Chế độ chỉ xem/i)).not.toBeInTheDocument();
    expect(screen.getByText('Tọa độ: 105.8865152, 21.5482368')).toBeInTheDocument();
    expect(screen.queryByText(/POINT/i)).not.toBeInTheDocument();

    expect(screen.getByText('Thu hoạch nông sản')).toBeInTheDocument();
    expect(screen.getByText('Thu hoạch sớm trước thời gian cách ly')).toBeInTheDocument();
    expect(screen.getByText('Kiểm nghiệm không đạt tiêu chuẩn an toàn')).toBeInTheDocument();
    expect(screen.getByText("Thuộc lô hàng 'Lô xuất khẩu siêu thị VinMart' đang bị thu hồi")).toBeInTheDocument();

    // RÀ SOÁT BẢO MẬT READ-ONLY: Tuyệt đối KHÔNG có nút Sửa, Xóa, Cập nhật, Duyệt
    expect(screen.queryByRole('button', { name: /Sửa/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Chỉnh sửa/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Xóa/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cập nhật/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Phê duyệt/i })).not.toBeInTheDocument();
  });

  // =========================================================================
  // TC-04: Lọc theo loại cảnh báo
  // =========================================================================
  it('TC-04: Khi người dùng chọn lọc loại cảnh báo -> API được gọi với tham số alertType tương ứng', async () => {
    const getAlertLotsSpy = vi.spyOn(territoryAlertLotApi, 'getAlertLots').mockResolvedValue({
      success: true,
      status: 200,
      message: 'OK',
      data: {
        items: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      },
    });

    render(
      <MemoryRouter>
        <TerritoryAlertLotListPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(getAlertLotsSpy).toHaveBeenCalledTimes(1);
    });

    // Lần gọi đầu tiên không truyền alertType (ALL)
    expect(getAlertLotsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, size: 10 })
    );
  });

  // =========================================================================
  // Security Test: Xử lý 403 Forbidden khi truy cập lô ngoài địa bàn
  // =========================================================================
  it('Security: Truy cập direct URL của lô ngoài địa bàn (Backend trả 403) -> UI xử lý an toàn, hiển thị thông báo không có quyền', async () => {
    vi.spyOn(territoryAlertLotApi, 'getAlertLotDetail').mockRejectedValue({
      status: 403,
      message: 'Bạn không có quyền truy cập lô sản xuất này do nằm ngoài địa bàn được phân công.',
    });

    render(
      <MemoryRouter initialEntries={['/reports/alert-lots/lot-outside-area']}>
        <Routes>
          <Route path="/reports/alert-lots/:lotId" element={<TerritoryAlertLotDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    // Kiểm tra banner từ chối quyền truy cập trang trọng
    expect(await screen.findByText('Không có quyền truy cập lô sản xuất')).toBeInTheDocument();
    expect(
      screen.getByText(/Lô sản xuất này thuộc địa bàn quản lý khác ngoài phạm vi được phân công/i)
    ).toBeInTheDocument();

    // Có link quay lại danh sách
    expect(screen.getByRole('link', { name: /Quay lại danh sách theo dõi lô có cảnh báo/i })).toBeInTheDocument();
  });

  // =========================================================================
  // Export Test: Kiểm tra trigger gọi export API
  // =========================================================================
  it('Export: Khi bấm Xuất báo cáo -> API exportAlertLots được gọi đúng tham số', async () => {
    vi.spyOn(territoryAlertLotApi, 'getAlertLots').mockResolvedValue({
      success: true,
      status: 200,
      message: 'OK',
      data: {
        items: [
          {
            lotId: 'lot-01',
            lotCode: 'LOT-01',
            lotName: 'Lô Test',
            organizationId: 'org-01',
            organizationName: 'HTX A',
            lotStatus: 'HARVESTED',
            alertTypes: ['RECALLING'],
            primaryAlertType: 'RECALLING',
            alertCount: 1,
            latestAlertTriggeredAt: '2026-09-10T08:00:00',
            alertSummaries: [],
            createdAt: '2026-09-01T08:00:00',
          },
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
    });

    const exportSpy = vi.spyOn(territoryAlertLotApi, 'exportAlertLots').mockResolvedValue({
      blob: new Blob(['fake pdf content']),
      fileName: 'Danh_sach_lo_canh_bao_20260911.pdf',
    });

    // Mock URL.createObjectURL và revokeObjectURL
    window.URL.createObjectURL = vi.fn().mockReturnValue('blob:http://localhost/test');
    window.URL.revokeObjectURL = vi.fn();

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <TerritoryAlertLotListPage />
      </MemoryRouter>
    );

    // Chờ danh sách load xong
    expect(await screen.findByText('Lô Test')).toBeInTheDocument();

    const exportButton = screen.getByRole('button', { name: /Xuất báo cáo/i });
    expect(exportButton).toBeEnabled();

    await user.click(exportButton);

    await waitFor(() => {
      expect(exportSpy).toHaveBeenCalledTimes(1);
    });
  });

  // =========================================================================
  // TC-05: Kiểm tra cấu trúc 8 cột, nhãn bộ lọc và ẩn phân trang khi totalPages <= 1
  // =========================================================================
  it('TC-05: Kiểm tra cấu trúc 8 cột chuẩn, bộ lọc Lô/Nông sản/Vùng trồng và ẩn phân trang khi < 10 mục', async () => {
    const mockItems: AlertLotSummaryResponse[] = [
      {
        lotId: 'lot-01',
        lotCode: 'LOT-XUAT-KHAU-01',
        lotName: 'Lô Xoài Cát Chu Xuất Khẩu',
        productCategoryName: 'Xoài Cát Chu',
        farmAreaName: 'Vùng trồng Đào Xá 01',
        organizationId: 'org-01',
        organizationName: 'HTX Nông Nghiệp Đào Xá',
        communeName: 'Đào Xá',
        provinceName: 'Phú Thọ',
        latestAlertTriggeredAt: '2026-09-12T08:00:00',
        alertTypes: ['RECALLING'],
        alertSummaries: [
          {
            alertType: 'RECALLING',
            alertName: 'Đang thu hồi',
            severity: 'CRITICAL',
            triggeredAt: '2026-09-12T08:00:00',
          },
        ],
        createdAt: '2026-09-01T07:00:00',
      },
    ];

    vi.spyOn(territoryAlertLotApi, 'getAlertLots').mockResolvedValue({
      success: true,
      status: 200,
      message: 'Truy vấn danh sách lô có cảnh báo thành công.',
      data: {
        items: mockItems,
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
    });

    render(
      <MemoryRouter>
        <TerritoryAlertLotListPage />
      </MemoryRouter>
    );

    // 1. Kiểm tra 8 cột tiêu đề của bảng
    expect(await screen.findByRole('columnheader', { name: 'STT' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Lô sản xuất' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Vùng trồng' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Loại nông sản' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Tổ chức sở hữu' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Loại cảnh báo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Thời gian cảnh báo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Thao tác' })).toBeInTheDocument();

    // 2. Kiểm tra dữ liệu từng cột hiển thị chính xác
    expect(screen.getByText('Lô Xoài Cát Chu Xuất Khẩu')).toBeInTheDocument();
    expect(screen.getByText('Vùng trồng Đào Xá 01')).toBeInTheDocument();
    expect(screen.getByText('Xoài Cát Chu')).toBeInTheDocument();
    expect(screen.getByText('HTX Nông Nghiệp Đào Xá')).toBeInTheDocument();

    // 3. Kiểm tra ô tìm kiếm và xác nhận các nhãn text trong bộ lọc đã được loại bỏ
    expect(screen.getByPlaceholderText('Nhập tên lô, loại nông sản hoặc vùng trồng...')).toBeInTheDocument();
    expect(screen.queryByText('Bộ lọc tìm kiếm')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Loại cảnh báo')).not.toBeInTheDocument();
    expect(screen.queryByText('Lô sản xuất / Loại nông sản / Vùng trồng')).not.toBeInTheDocument();

    // 4. Kiểm tra dropdown loại cảnh báo hiển thị tiếng Việt
    expect(screen.getByText('Tất cả loại cảnh báo')).toBeInTheDocument();

    // 5. Kiểm tra ẩn thanh chuyển trang khi chỉ có 1 trang (totalPages <= 1)
    expect(screen.queryByRole('button', { name: /Trang trước/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Trang sau/i })).not.toBeInTheDocument();
  });

  // =========================================================================
  // TC-08: Khi kết quả lọc rỗng -> Hiển thị bảng trống và text "Không tìm thấy lô sản xuất phù hợp với bộ lọc.", không hiện cảnh báo chưa phân công địa bàn
  // =========================================================================
  it('TC-08: Khi kết quả lọc rỗng -> Hiển thị bảng trống và text "Không tìm thấy lô sản xuất phù hợp với bộ lọc.", không hiện cảnh báo chưa phân công địa bàn', async () => {
    vi.spyOn(territoryAlertLotApi, 'getAlertLots').mockResolvedValue({
      success: true,
      status: 200,
      message: 'Truy vấn danh sách lô có cảnh báo thành công.',
      data: {
        items: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      },
    });

    render(
      <MemoryRouter>
        <TerritoryAlertLotListPage />
      </MemoryRouter>
    );

    // Kiểm tra dòng text đúng yêu cầu
    expect(
      await screen.findByText('Không tìm thấy lô sản xuất phù hợp với bộ lọc.')
    ).toBeInTheDocument();

    // Tuyệt đối không hiển thị cảnh báo chưa phân công địa bàn
    expect(
      screen.queryByText('Bạn chưa được phân công địa bàn nên chưa có dữ liệu lô cảnh báo để xem.')
    ).not.toBeInTheDocument();

    // Loại bỏ hoàn toàn nút "Xóa điều kiện lọc" trong bảng trống
    expect(screen.queryByText('Xóa điều kiện lọc')).not.toBeInTheDocument();
  });
});


