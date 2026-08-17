/**
 * Mock upload phiếu kết quả kiểm nghiệm.
 *
 * Backend chưa có endpoint upload cho phiếu kết quả kiểm nghiệm
 * (NCL-11-CN-003); tạm thời trả URL giả. Khi backend bổ sung endpoint
 * thật, thay phần thân hàm bằng axios upload và trả URL từ server.
 */
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const getExtension = (fileName: string): string => {
  const dotIndex = fileName.lastIndexOf(".");
  return dotIndex >= 0 ? fileName.slice(dotIndex + 1).toLowerCase() : "pdf";
};

/**
 * Giả lập upload file phiếu kết quả, trả về URL giả.
 */
export const mockUploadInspectionResultFile = async (
  file: File
): Promise<string> => {
  await delay(600);
  const extension = getExtension(file.name);
  return `https://storage.nguongocso.vn/inspection/mock-${crypto.randomUUID()}.${extension}`;
};