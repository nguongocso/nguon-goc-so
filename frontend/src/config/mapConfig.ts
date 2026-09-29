/**
 * CẤU HÌNH BẢN ĐỒ DÙNG CHUNG (MAP TILE CONFIG)
 * ---------------------------------------------------------------------------
 * Sử dụng CartoDB Voyager raster tiles (nguồn dữ liệu OpenStreetMap):
 * 1. Hoàn toàn miễn phí, CDN Fastly toàn cầu với POP đặt tại Đông Nam Á (độ trễ < 20ms).
 * 2. Không bị chặn/đầu độc DNS tại Việt Nam (các nhà mạng VNPT, Viettel, FPT chặn domain tile.openstreetmap.org do chính sách kiểm soát bản đồ).
 * 3. Hỗ trợ hiển thị tiếng Việt đầy đủ, độ tương phản hài hoà, tối ưu cho ứng dụng nông nghiệp và chuỗi cung ứng.
 */
export const MAP_CONFIG = {
  TILE_URL: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
  ATTRIBUTION:
    '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions" target="_blank" rel="noopener noreferrer">CARTO</a>',
  SUBDOMAINS: 'abcd',
  MAX_ZOOM: 20,
};
