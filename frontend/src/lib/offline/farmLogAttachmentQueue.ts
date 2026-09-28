import { v4 as uuidv4 } from 'uuid';
import { nenAnh } from '@/utils/anhNen';
import {
  MAX_ANH_MOI_NHAT_KY,
  capNhatTrangThaiTep,
  demTepCho,
  layTepTheoNhatKy,
  luuTepDinhKem,
  xoaTepDinhKem,
  xoaTepTheoNhatKy,
  type TepDinhKem,
} from '@/lib/offline/farmLogDb';

/**
 * Hàng chờ ảnh/đính kèm của nhật ký ghi ngoại tuyến (NCL-10-CN-012).
 *
 * Từ v2.4.0, nội dung nhật ký đi qua hàng chờ chung (`services/offlineQueue`)
 * nên module này chỉ quản lý ảnh/đính kèm (dạng Blob trong IndexedDB):
 * ảnh được nén ngay khi người dùng chụp/chọn, rồi tải lên endpoint đính kèm
 * sau khi nội dung đã được ghi (đúng task `NCL-10-CN-012-CV-03`).
 * Ảnh và bản ghi chờ liên kết với nhau qua `offlineEventId`.
 */

export {
  MAX_ANH_MOI_NHAT_KY,
  capNhatTrangThaiTep,
  demTepCho,
  layTepTheoNhatKy,
  xoaTepDinhKem,
  xoaTepTheoNhatKy,
  type TepDinhKem,
};

const DINH_DANG_CHO_PHEP = ['image/jpeg', 'image/png', 'application/pdf'];
const DUNG_LUONG_TOI_DA = 5 * 1024 * 1024;

/** Đổi tên tệp sang đuôi `.jpg` khi ảnh đã được nén về JPEG. */
function doiTenSauNen(ten: string, loai: string): string {
  if (loai === 'image/jpeg' && !/\.jpe?g$/i.test(ten)) {
    return ten.replace(/\.[^.]+$/, '') + '.jpg';
  }
  return ten;
}

/**
 * Nén và lưu danh sách ảnh cho một nhật ký chờ trong hàng chung.
 *
 * @param offlineEventId ID bản ghi chờ trong hàng chung (khóa liên kết với ảnh)
 * @param files          ảnh người dùng chụp/chọn
 * @returns số ảnh đã lưu
 * @throws Lỗi khi vượt quá số lượng/dung lượng cho phép hoặc hết bộ nhớ thiết bị.
 */
export async function luuAnhChoNhatKy(
  offlineEventId: string,
  files: File[],
): Promise<number> {
  const daLuu = (await layTepTheoNhatKy(offlineEventId)).length;
  if (daLuu + files.length > MAX_ANH_MOI_NHAT_KY) {
    throw new Error(`Chỉ được chọn tối đa ${MAX_ANH_MOI_NHAT_KY} chứng từ.`);
  }

  let soLuu = 0;
  for (const goc of files) {
    const daNen = await nenAnh(goc);
    if (!DINH_DANG_CHO_PHEP.includes(daNen.type) || daNen.size > DUNG_LUONG_TOI_DA) {
      throw new Error(`Tệp "${goc.name}" không hợp lệ hoặc vượt quá 5MB sau khi nén.`);
    }
    await luuTepDinhKem({
      id: uuidv4(),
      offlineEventId,
      ten: doiTenSauNen(goc.name, daNen.type),
      loai: daNen.type,
      kichThuoc: daNen.size,
      blob: daNen,
    });
    soLuu += 1;
  }

  return soLuu;
}

/**
 * Lấy toàn bộ ảnh còn lưu, kể cả ảnh gửi dở khi ứng dụng bị đóng.
 */
export async function layAnhChuaGui(offlineEventId: string): Promise<TepDinhKem[]> {
  return layTepTheoNhatKy(offlineEventId);
}

/**
 * Dọn toàn bộ ảnh còn lại của một nhật ký sau khi đã gửi thành công.
 *
 * @returns số ảnh đã xoá
 */
export async function donAnhSauKhiGui(offlineEventId: string): Promise<number> {
  return xoaTepTheoNhatKy(offlineEventId);
}
