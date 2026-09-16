import apiClient from './axiosConfig';

export interface DossierCheckResponse {
  shipmentId: string;
  eligible: boolean;
  missingDocuments: string[];
}

/**
 * Kiểm tra điều kiện xuất hồ sơ
 */
export const checkDossierEligibility = async (shipmentId: string): Promise<DossierCheckResponse> => {
  try {
    const response = await apiClient.get<{ data: DossierCheckResponse }>(
      `/shipments/${shipmentId}/dossier/check`
    );
    return response.data.data;
  } catch (error: any) {
    // Khi lô hàng chưa đủ chứng từ (QTN-11), backend ném DossierValidationException (HTTP 400)
    // kèm danh sách chứng từ thiếu trong trường 'errors' hoặc 'message'.
    if (error.response?.status === 400 && error.response?.data) {
      const data = error.response.data;
      const missingDocs: string[] = Array.isArray(data.errors)
        ? data.errors
        : Array.isArray(data.data?.missingDocuments)
        ? data.data.missingDocuments
        : [data.message || 'Chưa đủ chứng từ bắt buộc để xuất hồ sơ'];

      return {
        shipmentId,
        eligible: false,
        missingDocuments: missingDocs,
      };
    }
    throw error;
  }
};

/**
 * Xuất và tải hồ sơ PDF
 */
export const exportDossier = async (shipmentId: string, templateId?: string): Promise<Blob> => {
  try {
    const params: Record<string, string> = {};
    if (templateId && templateId !== 'default') {
      params.templateId = templateId;
    }
    const response = await apiClient.get(`/shipments/${shipmentId}/dossier/export`, {
      params,
      responseType: 'blob',
      timeout: 30000,
    });
    return response.data;
  } catch (error: any) {
    if (error.response?.data instanceof Blob && error.response.data.type?.includes('application/json')) {
      const text = await error.response.data.text();
      let message = text || 'Không đủ điều kiện hoặc lỗi khi tạo hồ sơ truy xuất';
      try {
        const errJson = JSON.parse(text);
        if (errJson?.message) {
          message = errJson.message;
        }
      } catch {
        // Không phải JSON hợp lệ → giữ nguyên text
      }
      throw new Error(message);
    }
    throw error;
  }
};

export interface Gs1EventLocation {
  latitude: number | null;
  longitude: number | null;
  address: string | null;
}

export interface Gs1Event {
  eventId: string;
  eventType: string;
  eventTypeLabel: string;
  recordedAt: string;
  recordedBy: string;
  location: Gs1EventLocation | null;
  details: Record<string, unknown> | null;
}

export interface Gs1ShipmentInfo {
  id: string;
  name: string;
  codeValues: string[] | null;
  productCategory: string | null;
  totalQuantity: number | null;
  unit: string | null;
  status: string;
  organization: {
    id: string;
    name: string;
    code: string;
  } | null;
}

export interface Gs1Warning {
  eventId: string | null;
  field: string;
  message: string;
}

export interface Gs1DossierExportResponse {
  shipment: Gs1ShipmentInfo;
  events: Gs1Event[];
  mapping: Record<string, string> | null;
  warnings: Gs1Warning[];
  exportedAt: string;
  exportedBy: string;
  schemaVersion: string;
  schemaDescription: string;
}

/**
 * Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng (JSON hoặc XML).
 * Chỉ dành cho VT-02 (Quản lý HTX) và VT-04 (Doanh nghiệp thu mua).
 *
 * @param shipmentId     ID lô hàng
 * @param format         'json' | 'xml' (mặc định json)
 * @param includeMapping có kèm bảng ánh xạ schema hay không (mặc định true)
 */
export const exportGs1Dossier = async (
  shipmentId: string,
  format: 'json' | 'xml' = 'json',
  includeMapping = true,
): Promise<{ blob: Blob; fileName: string }> => {
  try {
    const response = await apiClient.get(`/shipments/${shipmentId}/dossier/gs1`, {
      params: { format, includeMapping },
      responseType: 'blob',
      timeout: 30000,
    });

    const contentDisposition = response.headers?.['content-disposition'];
    let fileName = `GS1_Ho_so_truy_xuat_${shipmentId}.${format}`;

    if (contentDisposition) {
      const match = String(contentDisposition).match(
        /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/,
      );
      if (match && match[1]) {
        fileName = match[1].replace(/['"]/g, '');
      }
    }

    return { blob: response.data as Blob, fileName };
  } catch (error: any) {
    if (error.response?.data instanceof Blob && error.response.data.type?.includes('application/json')) {
      const text = await error.response.data.text();
      let message = text || 'Lỗi khi tạo hồ sơ GS1';
      try {
        const errJson = JSON.parse(text);
        if (errJson?.message) {
          message = errJson.message;
        }
      } catch {
        // Không phải JSON hợp lệ → giữ nguyên text
      }
      throw new Error(message);
    }
    throw error;
  }
};

// =========================================================================
// NCL-07-CN-005: Xuất hồ sơ truy xuất cho nhiều lô trong một lần (Batch Dossier Export)
// =========================================================================

export interface BatchShipmentEligibilityItem {
  shipmentId: string;
  shipmentName: string;
  eligible: boolean;
  missingDocuments: string[];
  organizationId?: string;
  organizationName?: string;
}

export interface BatchDossierCheckResponse {
  totalSelected: number;
  totalEligible: number;
  totalIneligible: number;
  eligibleShipments: BatchShipmentEligibilityItem[];
  ineligibleShipments: BatchShipmentEligibilityItem[];
}

export interface BatchDossierExportRequest {
  shipmentIds: string[];
  title?: string;
  note?: string;
  /** Mẫu hồ sơ áp dụng (NCL-07-CN-007); bỏ trống → dùng mẫu mặc định của tổ chức hoặc bộ trường chuẩn */
  templateId?: string;
}

export interface BatchDossierHistoryDto {
  id: string;
  title: string;
  exportedAt: string;
  exporterName: string;
  organizationName: string;
  totalSelectedLots: number;
  eligibleLotsCount: number;
  ineligibleLotsCount: number;
  fileName: string;
  fileSize: number;
  status: string;
  ipAddress: string;
  /** Mẫu hồ sơ đã áp dụng cho lần xuất này (nếu có) */
  templateId?: string | null;
}

/**
  * Kiểm tra điều kiện xuất hồ sơ hàng loạt cho danh sách lô (QTN-11 & QTN-01)
  */
export const checkBatchDossierEligibility = async (shipmentIds: string[]): Promise<BatchDossierCheckResponse> => {
  const response = await apiClient.post<{ data: BatchDossierCheckResponse }>(
    '/shipments/dossiers/batch-check',
    { shipmentIds }
  );
  return response.data.data;
};

/**
  * Xuất bộ hồ sơ PDF hợp nhất cho các lô đủ điều kiện
  */
export const exportBatchDossier = async (request: BatchDossierExportRequest): Promise<Blob> => {
  try {
    const response = await apiClient.post('/shipments/dossiers/batch-export', request, {
      responseType: 'blob',
      timeout: 60000,
    });
    return response.data;
  } catch (error: any) {
    if (error.response?.data instanceof Blob && error.response.data.type?.includes('application/json')) {
      const text = await error.response.data.text();
      let message = text || 'Lỗi khi xuất bộ hồ sơ truy xuất hàng loạt';
      try {
        const errJson = JSON.parse(text);
        if (errJson?.message) {
          message = errJson.message;
        }
      } catch {
        // Không phải JSON hợp lệ → giữ nguyên text
      }
      throw new Error(message);
    }
    throw error;
  }
};

/**
  * Lấy lịch sử xuất bộ hồ sơ truy xuất của tổ chức
  */
export const getBatchDossierExportHistory = async (): Promise<BatchDossierHistoryDto[]> => {
  const response = await apiClient.get<{ data: BatchDossierHistoryDto[] }>(
    '/shipments/dossiers/batch-history'
  );
  return response.data.data;
};

