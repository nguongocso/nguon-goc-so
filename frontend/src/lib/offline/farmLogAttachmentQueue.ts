import { v4 as uuidv4 } from 'uuid';
import { nenAnh } from '@/utils/anhNen';
import { luuNhatKyVaTepNguyenTu, type NhatKyChoMoi } from '@/lib/offline/farmLogDb';
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
 * Ảnh được nén ngay khi người dùng chụp/chọn rồi lưu dạng Blob trong IndexedDB;
 * phần dữ liệu văn bản luôn được gửi lên trước, ảnh gửi sau bằng endpoint đính kèm
 * (đúng task `NCL-10-CN-012-CV-03`).
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

/** Nén trước, sau đó lưu nội dung và toàn bộ tệp trong cùng một giao dịch. */
export async function luuNhatKyKemTep(nhatKy: NhatKyChoMoi, files: File[]): Promise<string> {
  if (files.length > MAX_ANH_MOI_NHAT_KY) {
    throw new Error(`Chỉ được chọn tối đa ${MAX_ANH_MOI_NHAT_KY} chứng từ.`);
  }
  const offlineEventId = nhatKy.offlineEventId ?? uuidv4();
  const danhSach: TepDinhKem[] = [];
  for (const file of files) {
    const daNen = await nenAnh(file);
    if (!['image/jpeg', 'image/png', 'application/pdf'].includes(daNen.type) || daNen.size > 5 * 1024 * 1024) {
      throw new Error(`Tệp "${file.name}" không hợp lệ hoặc vượt quá 5MB sau khi nén.`);
    }
    danhSach.push({
      id: uuidv4(), offlineEventId, ten: doiTenSauNen(file.name, daNen.type),
      loai: daNen.type, kichThuoc: daNen.size, blob: daNen, trangThai: 'cho',
    });
  }
  await luuNhatKyVaTepNguyenTu({ ...nhatKy, offlineEventId }, danhSach);
  return offlineEventId;
}

/** Đổi tên tệp sang đuôi `.jpg` khi ảnh đã được nén về JPEG. */
function doiTenSauNen(ten: string, loai: string): string {
  if (loai === 'image/jpeg' && !/\.jpe?g$/i.test(ten)) {
    return ten.replace(/\.[^.]+$/, '') + '.jpg';
  }
  return ten;
}

/**
 * Nén và lưu danh sách ảnh cho một nhật ký chờ.
 *
 * @param offlineEventId ID bản ghi chờ (khoá chống trùng của nhật ký)
 * @param files          ảnh người dùng chụp/chọn
 * @returns số ảnh đã lưu
 */
export async function luuAnhChoNhatKy(
  offlineEventId: string,
  files: File[],
): Promise<number> {
  const soLuong = Math.min(files.length, MAX_ANH_MOI_NHAT_KY);
  let soLuu = 0;

  for (let i = 0; i < soLuong; i += 1) {
    const goc = files[i];
    const daNen = await nenAnh(goc);
    await luuTepDinhKem({
      offlineEventId,
      ten: doiTenSauNen(daNen.name, daNen.type),
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