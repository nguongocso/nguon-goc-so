import { layDanhSachCho, xoaNhatKyCho } from '@/lib/offline/farmLogDb';
import { restoreOfflineEvent, thongBaoDoiHangCho } from '@/services/offlineQueue';

/**
 * Di chuyển một lần các bản ghi chờ cũ trong IndexedDB (`nhat-ky-cho`) sang
 * hàng chờ chung (`services/offlineQueue`, localStorage).
 *
 * Bối cảnh: trước v2.4.0, chức năng "Ghi nhật ký canh tác" ngoại tuyến tự xây
 * kho IndexedDB riêng nên bản ghi không hiện ở "Quản lý sự kiện chờ đồng bộ".
 * Từ v2.4.0 mọi bản ghi mới đi qua hàng chờ chung; hàm này cứu dữ liệu cũ còn
 * sót trên thiết bị (giữ nguyên trạng thái, số lần thử và lý do lỗi).
 *
 * Ảnh/đính kèm không cần di chuyển vì vẫn nằm cùng kho `tep-dinh-kem`,
 * liên kết qua `offlineEventId` không đổi.
 *
 * @returns Số bản ghi đã di chuyển trong lần gọi này.
 */
let daChayTrongPhien = false;

export async function diChuyenHangChoFarmLogCu(): Promise<number> {
  if (daChayTrongPhien) return 0;
  daChayTrongPhien = true;

  let daChuyen = 0;
  try {
    const banGhiCu = await layDanhSachCho();
    // Ghi cũ trước để giữ đúng thứ tự xếp hàng.
    for (const banGhi of [...banGhiCu].reverse()) {
      const khoiPhuc = {
        ...banGhi,
        // Bản ghi kẹt `syncing` (đóng tab giữa lúc gửi) đưa về `pending`;
        // bản `da-ghi` thiếu `farmLogId` cũng gửi lại nội dung.
        status:
          banGhi.status === 'syncing' ||
          (banGhi.status === 'da-ghi' && !banGhi.farmLogId)
            ? ('pending' as const)
            : banGhi.status,
      };
      let ketQua: 'ok' | 'duplicate' | 'full';
      try {
        ketQua = restoreOfflineEvent(khoiPhuc);
      } catch {
        // localStorage lỗi (hết dung lượng): dừng, giữ lại phần còn lại.
        break;
      }
      if (ketQua === 'full') break;
      try {
        await xoaNhatKyCho(banGhi.offlineEventId);
      } catch {
        // Xóa kho cũ lỗi: dừng để lần sau thử lại, tránh nhân đôi.
        break;
      }
      if (ketQua === 'ok') daChuyen += 1;
    }
  } catch {
    // IndexedDB lỗi: bỏ qua, không chặn luồng đồng bộ chuẩn.
    return 0;
  }

  if (daChuyen > 0) thongBaoDoiHangCho();
  return daChuyen;
}
