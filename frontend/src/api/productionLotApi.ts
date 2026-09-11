import apiClient from './axiosConfig';

// =========================================================
// NCL-11-CN-005: Xử lý lô không đạt kiểm nghiệm
// =========================================================

import type {
    ApproveProductionLotRequest,
    ApproveProductionLotResult,
    CancelProductionLotRequest,
    CloneProductionLotPreview,
    CloneProductionLotRequest,
    CloneProductionLotResponse,
    CreateProductionLotRequest,
    CreateProductionLotResponse,
    DisposeProductionLotRequest,
    DisposeProductionLotResponse,
    FarmAreaOption,
    ProductCategoryOption,
    ProductionLot,
    UpdateProductionLotRequest,
    UpdateProductionLotResponse,
    ChainProgressBoardData,
} from '@/types/productionLot';

/**
 * Loại bỏ lô sản xuất không đạt kiểm nghiệm.
 *
 * POST /api/v1/production-lots/{id}/dispose
 *
 * Chỉ VT-02 được loại bỏ. Lý do và biện pháp xử lý là bắt buộc (TC-03).
 * Lô chuyển sang trạng thái cuối DISPOSED; không tạo lô hàng và không tính vào sản lượng dự kiến.
 */
export const disposeProductionLot = async (
    id: string,
    payload: DisposeProductionLotRequest,
): Promise<DisposeProductionLotResponse> => {
    const response = await apiClient.post<
        ApiDataResponse<DisposeProductionLotResponse>
    >(`/production-lots/${id}/dispose`, payload);

    return response.data.data;
};

import type {
    ProductionLotImportResultResponse,
    ProductionLotImportHistory,
} from '@/types/productionLotImport';

// =========================================================
// API RESPONSE
// =========================================================

interface ApiDataResponse<T> {
    data: T;
}

// =========================================================
// FARM AREA
// =========================================================

/**
 * Lấy danh sách vùng trồng để chọn khi nhập lô sản xuất.
 *
 * GET /api/v1/farm-areas
 */
export const getFarmAreaOptions = async (): Promise<FarmAreaOption[]> => {
    const response = await apiClient.get<ApiDataResponse<FarmAreaOption[]>>(
        '/farm-areas',
        { params: { activeOnly: true } }
    );

    return response.data.data;
};

// =========================================================
// PRODUCT CATEGORY
// =========================================================

/**
 * Lấy danh sách loại nông sản.
 *
 * GET /api/v1/product-categories
 */
export const getProductCategoryOptions =
    async (): Promise<ProductCategoryOption[]> => {
        const response = await apiClient.get<
            ApiDataResponse<ProductCategoryOption[]>
        >('/product-categories');

        return response.data.data;
    };

// =========================================================
// CREATE PRODUCTION LOT
// =========================================================

export const createProductionLot = async (
    payload: CreateProductionLotRequest,
): Promise<CreateProductionLotResponse> => {
    const response = await apiClient.post<
        ApiDataResponse<CreateProductionLotResponse>
    >('/production-lots', payload);

    return response.data.data;
};

// =========================================================
// GET PRODUCTION LOTS
// =========================================================

export const getProductionLots = async (
    status?: ProductionLot['status'],
): Promise<ProductionLot[]> => {
    const response = await apiClient.get<ApiDataResponse<ProductionLot[]>>(
        '/production-lots',
        {
            params: status ? { status } : undefined,
        },
    );

    return response.data.data;
};

// =========================================================
// GET PRODUCTION LOT BY ID
// =========================================================

export const getProductionLotById = async (
    id: string,
): Promise<ProductionLot> => {
    const response = await apiClient.get<ApiDataResponse<ProductionLot>>(
        `/production-lots/${id}`,
    );

    return response.data.data;
};

// =========================================================
// UPDATE PRODUCTION LOT
// =========================================================

export const updateProductionLot = async (
    id: string,
    data: UpdateProductionLotRequest,
): Promise<UpdateProductionLotResponse> => {
    const response = await apiClient.put<
        ApiDataResponse<UpdateProductionLotResponse>
    >(`/production-lots/${id}`, data);

    return response.data.data;
};

// =========================================================
// SUBMIT PRODUCTION LOT
// =========================================================

export const submitProductionLot = async (
    id: string,
): Promise<ProductionLot> => {
    const response = await apiClient.post<ApiDataResponse<ProductionLot>>(
        `/production-lots/${id}/submit`,
    );

    return response.data.data;
};

// =========================================================
// APPROVE PRODUCTION LOT
// =========================================================

export const approveProductionLot = async (
    id: string,
    payload: ApproveProductionLotRequest,
): Promise<ApproveProductionLotResult> => {
    const response = await apiClient.post<
        ApiDataResponse<ApproveProductionLotResult>
    >(`/production-lots/${id}/approve`, payload);

    return response.data.data;
};

// =========================================================
// CANCEL PRODUCTION LOT (NCL-02-CN-006)
// =========================================================

export const cancelProductionLot = async (
    id: string,
    payload: CancelProductionLotRequest,
): Promise<ProductionLot> => {
    const response = await apiClient.post<ApiDataResponse<ProductionLot>>(
        `/production-lots/${id}/cancel`,
        payload,
    );

    return response.data.data;
};

// =========================================================
// CLONE PRODUCTION LOT FROM PREVIOUS SEASON (NCL-02-CN-007)
// =========================================================

/**
 * Lấy dữ liệu xem trước khi tạo lô sản xuất mới từ mẫu vụ trước.
 *
 * GET /api/v1/production-lots/{sourceLotId}/clone-preview
 *
 * Chỉ VT-02. Response chỉ chứa dữ liệu nền cần cho form, không expose
 * lịch sử vận hành của lô mẫu.
 */
export const getCloneProductionLotPreview = async (
    sourceLotId: string,
): Promise<CloneProductionLotPreview> => {
    const response = await apiClient.get<
        ApiDataResponse<CloneProductionLotPreview>
    >(`/production-lots/${sourceLotId}/clone-preview`);

    return response.data.data;
};

/**
 * Tạo lô sản xuất mới từ mẫu vụ trước.
 *
 * POST /api/v1/production-lots/{sourceLotId}/clone
 *
 * Chỉ VT-02. Lô mới luôn ở trạng thái DRAFT, kế thừa vùng trồng / loại
 * nông sản / chứng nhận còn hiệu lực của lô mẫu.
 */
export const cloneProductionLot = async (
    sourceLotId: string,
    payload: CloneProductionLotRequest,
): Promise<CloneProductionLotResponse> => {
    const response = await apiClient.post<
        ApiDataResponse<CloneProductionLotResponse>
    >(`/production-lots/${sourceLotId}/clone`, payload);

    return response.data.data;
};

// =========================================================
// DASHBOARD
// =========================================================

export interface DashboardSummary {
    totalLots: number;
    totalExpectedYield: number;
    totalActualYield: number;
}

export type DashboardStatusCount = Record<string, number>;

export interface DashboardTimeSeriesItem {
    period: string;
    lotCount: number;
    expectedYield: number;
    actualYield: number;
}

export interface DashboardResponse {
    summary: DashboardSummary;
    byStatus: DashboardStatusCount;
    timeSeries: DashboardTimeSeriesItem[];
}

export const getProductionLotDashboard = async (params?: {
    startDate?: string;
    endDate?: string;
    organizationId?: string;
    groupBy?: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';
}): Promise<DashboardResponse> => {
    const response = await apiClient.get<ApiDataResponse<DashboardResponse>>(
        '/production-lots/dashboard',
        {
            params,
        },
    );

    return response.data.data;
};

// =========================================================
// IMPORT PRODUCTION LOTS
// NCL-10-CN-006
// =========================================================

/**
 * Nhập dữ liệu lô sản xuất từ file Excel.
 *
 * POST /api/v1/production-lots/import
 *
 * Content-Type:
 * multipart/form-data
 *
 * @param file File Excel .xlsx
 * @param organizationId ID tổ chức, không bắt buộc
 */
export const importProductionLots = async (
    file: File,
    organizationId?: string,
): Promise<ProductionLotImportResultResponse> => {
    const formData = new FormData();

    formData.append('file', file);

    if (organizationId) {
        formData.append('organizationId', organizationId);
    }

    const response = await apiClient.post<
        ApiDataResponse<ProductionLotImportResultResponse>
    >('/production-lots/import', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });

    return response.data.data;
};

// =========================================================
// IMPORT HISTORY
// =========================================================

/**
 * Lấy lịch sử nhập dữ liệu lô sản xuất.
 *
 * GET /api/v1/production-lots/import-history
 */
export const getImportHistory = async (): Promise<
    ProductionLotImportHistory[]
> => {
    const response = await apiClient.get<
        ApiDataResponse<ProductionLotImportHistory[]>
    >('/production-lots/import-history');

    return response.data.data;
};

// =========================================================
// DOWNLOAD IMPORT TEMPLATE
// =========================================================

/**
 * Tải file Excel mẫu nhập lô sản xuất.
 *
 * GET /api/v1/production-lots/import-template
 *
 * Backend yêu cầu:
 * - productCategoryId
 * - farmAreaId
 *
 * Backend trả về:
 * - mau_nhap_lo_san_xuat.xlsx
 */
export const downloadImportTemplate = async (
    productCategoryId: string,
    farmAreaId: string,
): Promise<void> => {
    const response = await apiClient.get<Blob>(
        '/production-lots/import-template',
        {
            params: {
                productCategoryId,
                farmAreaId,
            },
            responseType: 'blob',
        },
    );

    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });

    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');

    link.href = url;
    link.download = 'mau_nhap_lo_san_xuat.xlsx';

    document.body.appendChild(link);

    link.click();

    document.body.removeChild(link);

    window.URL.revokeObjectURL(url);
};

// =========================================================
// CHAIN PROGRESS BOARD (NCL-10-CN-013)
// =========================================================

/**
 * Lấy bảng theo dõi tiến độ chuỗi của từng lô (NCL-10-CN-013).
 *
 * GET /api/v1/production-lots/chain-progress
 */
export const getChainProgressBoard = async (params?: {
    organizationId?: string;
    stagnantThresholdDays?: number;
    search?: string;
}): Promise<ChainProgressBoardData> => {
    const response = await apiClient.get<ApiDataResponse<ChainProgressBoardData>>(
        '/production-lots/chain-progress',
        { params },
    );

    return response.data.data;
};