#!/usr/bin/env node
// ============================================================
// generate-admin-units.mjs (NCL-740)
//
// Sinh migration dữ liệu V36__seed_administrative_units.sql:
// seed TOÀN BỘ danh mục đơn vị hành chính Việt Nam theo mô hình 2 cấp
// hiệu lực từ 01/07/2025 (34 tỉnh/thành phố + toàn bộ xã/phường/đặc khu,
// ~3.300 dòng) theo bảng mã hành chính chính thức.
//
// NGUỒN DỮ LIỆU (công khai):
//   https://github.com/thanglequoc/vietnamese-provinces-database
//   file json/vn_only_simplified_json_generated_data_vn_units.json
//   (mã đơn vị theo Quyết định 19/2025/QĐ-TTg; kiểm chứng: 34 tỉnh,
//   3.321 xã/phường/đặc khu).
//
// SCHEMA UUID DETERMINISTIC (UUIDv5 - RFC 4122):
//   id = UUIDv5(namespace = '9e5d7a58-2d47-4b6e-93f2-0d1c4a5e6f70',
//               name     = 'admin-unit:' + code)
//   Chỉ phụ thuộc MÃ HÀNH CHÍNH -> chạy lại script nhiều lần cho ra cùng
//   bộ UUID, không phá vỡ FK trong các môi trường đã seed.
//
// Cách chạy (từ thư mục backend/):
//   node scripts/generate-admin-units.mjs                       # tự tải nguồn về cache rồi sinh SQL ra stdout
//   node scripts/generate-admin-units.mjs --out <file.sql>      # ghi SQL ra file
//   node scripts/generate-admin-units.mjs --source <data.json>  # dùng file nguồn cục bộ (không cần mạng)
//
// KHÔNG sửa tay file SQL sinh ra — luôn tái tạo bằng script này.
// ============================================================

import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const SOURCE_URL =
  'https://raw.githubusercontent.com/thanglequoc/vietnamese-provinces-database/master/json/vn_only_simplified_json_generated_data_vn_units.json';
const CACHE_FILE = resolve(__dirname, '.cache-vn-admin-units.json');
const NAMESPACE = '9e5d7a58-2d47-4b6e-93f2-0d1c4a5e6f70';
const BATCH_SIZE = 500;

function parseArgs(argv) {
  const args = { out: null, source: null };
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--out') args.out = argv[++i];
    else if (argv[i] === '--source') args.source = argv[++i];
    else throw new Error(`Tham số không hỗ trợ: ${argv[i]}`);
  }
  return args;
}

async function loadSourceData(sourcePath) {
  if (sourcePath) {
    console.error(`[generate] Dùng file nguồn cục bộ: ${sourcePath}`);
    return readFileSync(sourcePath, 'utf8');
  }
  try {
    return readFileSync(CACHE_FILE, 'utf8');
  } catch {
    /* chưa có cache -> tải */
  }
  console.error(`[generate] Đang tải nguồn: ${SOURCE_URL}`);
  const res = await fetch(SOURCE_URL);
  if (!res.ok) throw new Error(`Tải nguồn thất bại: HTTP ${res.status}`);
  const body = await res.text();
  writeFileSync(CACHE_FILE, body);
  return body;
}

/** UUIDv5 (SHA-1) từ namespace UUID + tên. */
function uuidV5(name) {
  const nsBytes = Buffer.from(NAMESPACE.replace(/-/g, ''), 'hex');
  const hash = createHash('sha1')
    .update(nsBytes)
    .update(Buffer.from(name, 'utf8'))
    .digest();
  hash[6] = (hash[6] & 0x0f) | 0x50; // version 5
  hash[8] = (hash[8] & 0x3f) | 0x80; // variant RFC 4122
  const hex = hash.subarray(0, 16).toString('hex');
  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20),
  ].join('-');
}

const uuidForCode = (code) => uuidV5(`admin-unit:${code}`);

/** Bỏ tiền tố hành chính để lấy tên trần ("Thành phố Hà Nội" -> "Hà Nội"). */
function bareName(fullName) {
  return fullName
    .replace(/^(Thành phố|Tỉnh|Phường|Xã|xã|Đặc khu)\s+/, '')
    .trim();
}

const esc = (s) => s.replace(/'/g, "''");

function validate(data) {
  if (!Array.isArray(data)) throw new Error('Dữ liệu nguồn phải là mảng các tỉnh.');
  if (data.length !== 34) throw new Error(`Kỳ vọng 34 tỉnh/thành, nhận được ${data.length}.`);
  const codes = new Set();
  let wardCount = 0;
  for (const p of data) {
    if (codes.has(p.Code)) throw new Error(`Mã tỉnh trùng: ${p.Code}.`);
    codes.add(p.Code);
    for (const w of p.Wards || []) {
      wardCount++;
      if (codes.has(w.Code)) throw new Error(`Mã xã trùng: ${w.Code}.`);
      codes.add(w.Code);
    }
  }
  if (wardCount < 3200 || wardCount > 3400) {
    throw new Error(`Số xã/phường bất thường (${wardCount}) — kỳ vọng ~3.200–3.400.`);
  }
  console.error(`[generate] Kiểm chứng OK: ${data.length} tỉnh, ${wardCount} xã/phường/đặc khu.`);
}

function buildRows(data) {
  const rows = [];
  for (const p of [...data].sort((a, b) => a.Code.localeCompare(b.Code))) {
    const provinceId = uuidForCode(p.Code);
    rows.push({
      id: provinceId,
      code: p.Code,
      name: bareName(p.FullName),
      level: 'PROVINCE',
      parentId: null,
      provinceId: null,
    });
    for (const w of [...(p.Wards || [])].sort((a, b) => a.Code.localeCompare(b.Code))) {
      rows.push({
        id: uuidForCode(w.Code),
        code: w.Code,
        name: bareName(w.FullName),
        level: 'COMMUNE',
        parentId: provinceId,
        provinceId,
      });
    }
  }
  return rows;
}

function rowValues(r) {
  return `('${r.id}', '${esc(r.code)}', '${esc(r.name)}', '${r.level}', ${
    r.parentId ? `'${r.parentId}'` : 'NULL'
  }, ${r.provinceId ? `'${r.provinceId}'` : 'NULL'}, TRUE)`;
}

function renderSql(rows) {
  const header = `-- ============================================================
-- V36: Seed danh mục đơn vị hành chính toàn quốc (NCL-740)
--      Mô hình 2 cấp hiệu lực 01/07/2025 (QĐ 19/2025/QĐ-TTg):
--      34 tỉnh/thành phố + toàn bộ xã/phường/đặc khu.
-- Depends on: administrative_units (V35)
-- ============================================================
--
-- File được SINH TỰ ĐỘNG bởi backend/scripts/generate-admin-units.mjs
-- (nguồn: github.com/thanglequoc/vietnamese-provinces-database, CC-BY-4.0 /
-- MIT — mã đơn vị theo Quyết định 19/2025/QĐ-TTg). KHÔNG sửa tay.
--
-- UUID deterministic kiểu UUIDv5: SHA-1(namespace
-- '9e5d7a58-2d47-4b6e-93f2-0d1c4a5e6f70' + 'admin-unit:' + mã hành chính)
-- -> chạy lại script cho ra cùng bộ id, không phá FK.
-- Tên lưu dạng trần (đã bỏ tiền tố Tỉnh/Thành phố/Phường/Xã/Đặc khu);
-- cấp đơn vị nằm ở cột level.
`;

  const batches = [];
  for (let i = 0; i < rows.length; i += BATCH_SIZE) {
    const batch = rows.slice(i, i + BATCH_SIZE);
    batches.push(
      `INSERT INTO administrative_units (id, code, name, level, parent_id, province_id, active) VALUES\n` +
        batch.map(rowValues).join(',\n') +
        ';'
    );
  }
  return header + '\n' + batches.join('\n\n') + '\n';
}

const args = parseArgs(process.argv.slice(2));
const raw = await loadSourceData(args.source);
validate(JSON.parse(raw));
const rows = buildRows(JSON.parse(raw));
const sql = renderSql(rows);

if (args.out) {
  writeFileSync(args.out, sql, 'utf8');
  const kb = (Buffer.byteLength(sql, 'utf8') / 1024).toFixed(1);
  console.error(`[generate] Đã ghi ${args.out} (${kb} KB, ${rows.length} dòng).`);
} else {
  process.stdout.write(sql);
}
