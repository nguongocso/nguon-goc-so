/**
 * CẤU HÌNH BẢN ĐỒ DÙNG CHUNG (MAP TILE CONFIG)
 * ---------------------------------------------------------------------------
 * Sử dụng OpenStreetMap France (Humanitarian HOT layer):
 * 1. Hoàn toàn MIỄN PHÍ, KHÔNG YÊU CẦU API KEY, KHÔNG CÓ WATERMARK ("API KEY REQUIRED").
 * 2. Tên miền `*.tile.openstreetmap.fr` KHÔNG bị các nhà mạng Việt Nam chặn/đầu độc DNS.
 * 3. Hỗ trợ hiển thị tiếng Việt đầy đủ, phân biệt rõ đường sá, địa hình nông nghiệp, phân lô.
 * 4. Băng thông ổn định, hỗ trợ CORS đầy đủ (Access-Control-Allow-Origin: *).
 */
export const MAP_CONFIG = {
  TILE_URL: 'https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png',
  ATTRIBUTION:
    '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors, Tiles style by <a href="https://www.hotosm.org/" target="_blank" rel="noopener noreferrer">HOT</a> hosted by <a href="https://openstreetmap.fr/" target="_blank" rel="noopener noreferrer">OSM France</a>',
  SUBDOMAINS: 'abc',
  MAX_ZOOM: 19,
};
