import {
  ATTACHMENT_MAX_SIZE,
} from '@/components/common/AttachmentUploader';

/**
 * Nén ảnh phía trình duyệt trước khi lưu tạm ngoại tuyến (NCL-10-CN-012-CV-03:
 * "ảnh được nén và gửi sau phần dữ liệu").
 *
 * Quy tắc:
 * - Chỉ xử lý ảnh (`image/*`); tệp PDF hoặc tệp không phải ảnh được giữ nguyên.
 * - Ảnh được thu nhỏ để cạnh dài không vượt quá {@link CANH_DAI_TOI_DA} và giảm dần
 *   chất lượng JPEG cho tới khi nhỏ hơn giới hạn của máy chủ.
 * - Nếu trình duyệt không hỗ trợ xử lý ảnh (hoặc lỗi bất kỳ), trả lại tệp gốc để
 *   không chặn người dùng ghi nhật ký.
 */

/** Cạnh dài tối đa của ảnh sau khi nén (px). */
export const CANH_DAI_TOI_DA = 1280;

/** Chất lượng JPEG khởi điểm. */
const CHAT_LUONG_BAN_DAU = 0.8;

/** Chất lượng JPEG nhỏ nhất được phép thử. */
const CHAT_LUONG_TOI_THIEU = 0.5;

/** Ngưỡng ảnh JPEG nhỏ sẵn sàng dùng, không cần nén lại. */
const NGUONG_ANH_NHO = 0.6;

/**
 * Kiểm tra tệp có phải ảnh cần xử lý hay không.
 */
export function laAnh(file: File): boolean {
  return file.type.startsWith('image/');
}

/**
 * Tính kích thước mới giữ nguyên tỉ lệ, bảo đảm cạnh dài không vượt `canhDai`.
 */
export function tinhKichThuocMoi(
  width: number,
  height: number,
  canhDai: number = CANH_DAI_TOI_DA,
): { width: number; height: number } {
  const canhDaiNhat = Math.max(width, height);
  if (canhDaiNhat <= canhDai || canhDaiNhat === 0) {
    return { width, height };
  }
  const tiLe = canhDai / canhDaiNhat;
  return {
    width: Math.max(1, Math.round(width * tiLe)),
    height: Math.max(1, Math.round(height * tiLe)),
  };
}

/**
 * Tạo nguồn ảnh từ File (ưu tiên `createImageBitmap`, fallback sang `<img>`).
 */
async function taoNguonAnh(
  file: File,
): Promise<{ width: number; height: number; ve: CanvasImageSource }> {
  if (typeof createImageBitmap === 'function') {
    const bitmap = await createImageBitmap(file);
    return { width: bitmap.width, height: bitmap.height, ve: bitmap };
  }

  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const anh = new Image();
    anh.onload = () => {
      URL.revokeObjectURL(url);
      resolve({ width: anh.naturalWidth, height: anh.naturalHeight, ve: anh });
    };
    anh.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Không đọc được ảnh.'));
    };
    anh.src = url;
  });
}

/**
 * Vẽ ảnh đã thu nhỏ ra canvas và xuất blob JPEG theo chất lượng chỉ định.
 */
async function veVaNen(
  nguon: { width: number; height: number; ve: CanvasImageSource },
  chatLuong: number,
): Promise<File | null> {
  const kichThuoc = tinhKichThuocMoi(nguon.width, nguon.height);
  const canvas = document.createElement('canvas');
  canvas.width = kichThuoc.width;
  canvas.height = kichThuoc.height;

  const ctx = canvas.getContext('2d');
  if (!ctx) return null;

  ctx.drawImage(nguon.ve, 0, 0, kichThuoc.width, kichThuoc.height);

  const blob = await new Promise<Blob | null>((resolve) =>
    canvas.toBlob((ketQua) => resolve(ketQua), 'image/jpeg', chatLuong),
  );
  if (!blob) return null;

  return new File([blob], doiDuoiJpg('anh.jpg'), {
    type: 'image/jpeg',
    lastModified: Date.now(),
  });
}

/** Đổi tên tệp sang đuôi `.jpg` sau khi nén về JPEG. */
function doiDuoiJpg(ten: string): string {
  return ten.replace(/\.(png|jpeg|jpg|webp|heic|heif)$/i, '.jpg');
}

/**
 * Nén một tệp ảnh trước khi lưu tạm ngoại tuyến.
 *
 * @param file     tệp người dùng chọn/chụp
 * @param maxBytes dung lượng tối đa cho phép (mặc định theo cấu hình máy chủ 5 MB)
 * @returns tệp đã nén (hoặc tệp gốc nếu không cần/không thể nén)
 */
export async function nenAnh(
  file: File,
  maxBytes: number = ATTACHMENT_MAX_SIZE,
): Promise<File> {
  if (!laAnh(file) || file.size === 0) return file;

  // Ảnh JPEG đã nhỏ sẵn thì giữ nguyên để không giảm chất lượng vô ích.
  if (file.type === 'image/jpeg' && file.size <= maxBytes * NGUONG_ANH_NHO) {
    return file;
  }

  try {
    const nguon = await taoNguonAnh(file);
    let chatLuong = CHAT_LUONG_BAN_DAU;
    let ketQua = await veVaNen(nguon, chatLuong);

    while (ketQua && ketQua.size > maxBytes && chatLuong > CHAT_LUONG_TOI_THIEU) {
      chatLuong -= 0.1;
      ketQua = await veVaNen(nguon, chatLuong);
    }

    if (!ketQua) return file;
    // Chỉ dùng bản nén khi thực sự nhỏ hơn tệp gốc.
    return ketQua.size < file.size ? ketQua : file;
  } catch {
    return file;
  }
}