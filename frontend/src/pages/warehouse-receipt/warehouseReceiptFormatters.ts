/** Định dạng ngày theo múi giờ hiển thị của người dùng. */
export function formatWarehouseReceiptDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  } catch {
    return iso;
  }
}

/** Định dạng ngày giờ theo múi giờ hiển thị của người dùng. */
export function formatWarehouseReceiptDateTime(iso: string): string {
  try {
    const date = new Date(iso);
    const formattedDate = date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
    const formattedTime = date.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
    });
    return `${formattedDate} ${formattedTime}`;
  } catch {
    return iso;
  }
}
