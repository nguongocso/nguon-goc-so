import React from 'react';

const TAPE_1_ITEMS = [
  'MINH BẠCH NGUỒN GỐC',
  'TIÊU CHUẨN QUỐC GIA',
  'VIETGAP CERTIFIED',
  'GLOBALG.A.P.',
  'ISO 22000',
  'CHUỖI CUNG ỨNG SỐ 4.0',
  'GS1 DIGITAL LINK',
  'XÁC THỰC THỜI GIAN THỰC',
];

const TAPE_2_ITEMS = [
  'NÔNG NGHIỆP CÔNG NGHỆ CAO',
  'TRUY XUẤT NÔNG SẢN VIỆT',
  'HACCP CODEX',
  'OCOP VIỆT NAM',
  'NHẬT KÝ SỐ AI',
  'BẢO VỆ NGƯỜI TIÊU DÙNG',
  'DỮ LIỆU BẤT BIẾN',
  'BẢO MẬT & MINH BẠCH',
];

export const DualMarqueeRibbon: React.FC = () => {
  return (
    <section
      aria-label="Tiêu chuẩn & Tôn chỉ nền tảng"
      className="relative w-full overflow-hidden bg-emerald-950 py-7 sm:py-9"
    >
      {/* Background ambient glow lines */}
      <div className="pointer-events-none absolute inset-0 -z-10 bg-gradient-to-b from-black/40 via-transparent to-black/40" />

      {/* Tilted Container - Street Tech Angled Ribbons */}
      <div className="relative -mx-8 -my-4 flex flex-col gap-3 -rotate-2 select-none">
        {/* ============================================================
            TAPE 1: Leftward Marquee (Dark Emerald with Glowing Text)
            ============================================================ */}
        <div className="relative w-full overflow-hidden border-y border-emerald-500/30 bg-emerald-900/90 py-2.5 shadow-md sm:py-3">
          <div className="animate-marquee-left flex items-center">
            {/* Repeat list twice to create seamless loop */}
            {[...TAPE_1_ITEMS, ...TAPE_1_ITEMS, ...TAPE_1_ITEMS, ...TAPE_1_ITEMS].map(
              (text, index) => (
                <span
                  key={`tape-1-${index}`}
                  className="inline-flex items-center text-xs font-black tracking-[0.2em] text-emerald-300 uppercase sm:text-sm"
                >
                  <span className="px-5">{text}</span>
                  <span className="text-emerald-500/60 font-mono">///</span>
                </span>
              ),
            )}
          </div>
        </div>

        {/* ============================================================
            TAPE 2: Rightward Marquee (High-Voltage Emerald Accent)
            ============================================================ */}
        <div className="relative w-full overflow-hidden bg-emerald-400 py-2.5 shadow-xl sm:py-3">
          <div className="animate-marquee-right flex items-center">
            {/* Repeat list twice to create seamless loop */}
            {[...TAPE_2_ITEMS, ...TAPE_2_ITEMS, ...TAPE_2_ITEMS, ...TAPE_2_ITEMS].map(
              (text, index) => (
                <span
                  key={`tape-2-${index}`}
                  className="inline-flex items-center text-xs font-black tracking-[0.2em] text-emerald-950 uppercase sm:text-sm"
                >
                  <span className="px-5">{text}</span>
                  <span className="text-emerald-800 font-mono">///</span>
                </span>
              ),
            )}
          </div>
        </div>
      </div>
    </section>
  );
};
