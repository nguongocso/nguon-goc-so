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
  const response = await apiClient.get<{ data: DossierCheckResponse }>(
    `/shipments/${shipmentId}/dossier/check`
  );
  return response.data.data;
};

/**
 * Xuất và tải hồ sơ PDF
 */
export const exportDossier = async (shipmentId: string): Promise<Blob> => {
  try {
    const response = await apiClient.get(`/shipments/${shipmentId}/dossier/export`, {
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

