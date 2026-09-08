import React, { useEffect, useState } from 'react';
import { getChainProgressBoard } from '@/api/productionLotApi';
import { ChainProgressBoard } from '@/components/production-lot/ChainProgressBoard';
import type { ChainProgressBoardData } from '@/types/productionLot';

/**
 * Trang Bảng theo dõi tiến độ chuỗi của từng lô (NCL-10-CN-013).
 * Thiết kế giao diện: Không modal, không icon, không màu sắc lòe loẹt.
 */
export const ChainProgressPage: React.FC = () => {
  const [data, setData] = useState<ChainProgressBoardData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState<string>('');
  const [stagnantThresholdDays, setStagnantThresholdDays] = useState<number>(10);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await getChainProgressBoard({
        stagnantThresholdDays,
        search: search.trim() || undefined,
      });
      setData(result);
    } catch (err: any) {
      console.error('Lỗi khi tải bảng tiến độ chuỗi:', err);
      setError(err?.response?.data?.message || 'Không thể tải dữ liệu tiến độ chuỗi.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [stagnantThresholdDays]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchData();
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'sans-serif', backgroundColor: '#ffffff', minHeight: '85vh' }}>
      {/* Tiêu đề trang */}
      <div style={{ marginBottom: '16px', borderBottom: '1px solid #e0e0e0', paddingBottom: '12px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#222222', margin: 0 }}>
          Bảng theo dõi tiến độ chuỗi của từng lô
        </h2>
        <div style={{ fontSize: '13px', color: '#666666', marginTop: '4px' }}>
          Tổng hợp tất cả lô đang mở theo 9 giai đoạn của chuỗi sản xuất &amp; lưu thông kèm việc cần làm tiếp theo.
        </div>
      </div>

      {/* Thanh công cụ lọc & tìm kiếm */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '16px',
          marginBottom: '16px',
          flexWrap: 'wrap',
          backgroundColor: '#f5f5f5',
          padding: '12px 16px',
          borderRadius: '4px',
          border: '1px solid #e5e5e5',
        }}
      >
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <label style={{ fontSize: '13px', color: '#333333' }}>Tìm kiếm:</label>
          <input
            type="text"
            placeholder="Tên lô hoặc vùng trồng..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              padding: '6px 10px',
              fontSize: '13px',
              border: '1px solid #ccc',
              borderRadius: '3px',
              width: '240px',
            }}
          />
          <button
            type="submit"
            style={{
              padding: '6px 12px',
              fontSize: '13px',
              backgroundColor: '#2b5797',
              color: '#ffffff',
              border: 'none',
              borderRadius: '3px',
              cursor: 'pointer',
            }}
          >
            Tìm kiếm
          </button>
        </form>

        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <label style={{ fontSize: '13px', color: '#333333' }}>Ngưỡng tồn đọng (ngày):</label>
            <select
              value={stagnantThresholdDays}
              onChange={(e) => setStagnantThresholdDays(Number(e.target.value))}
              style={{
                padding: '6px 10px',
                fontSize: '13px',
                border: '1px solid #ccc',
                borderRadius: '3px',
              }}
            >
              <option value={5}>5 ngày</option>
              <option value={7}>7 ngày</option>
              <option value={10}>10 ngày</option>
              <option value={14}>14 ngày</option>
              <option value={30}>30 ngày</option>
            </select>
          </div>

          <button
            onClick={fetchData}
            style={{
              padding: '6px 12px',
              fontSize: '13px',
              backgroundColor: '#ffffff',
              color: '#333333',
              border: '1px solid #ccc',
              borderRadius: '3px',
              cursor: 'pointer',
            }}
          >
            Tải lại
          </button>
        </div>
      </div>

      {/* Thông tin thống kê nhanh */}
      {data && (
        <div
          style={{
            display: 'flex',
            gap: '24px',
            marginBottom: '16px',
            fontSize: '13px',
            color: '#333333',
          }}
        >
          <div>
            Tổ chức: <strong>{data.organizationName}</strong>
          </div>
          <div>
            Tổng số lô đang mở: <strong>{data.totalOpenLots}</strong>
          </div>
          <div>
            Số lô tồn đọng: <strong style={{ color: data.stagnantLotsCount > 0 ? '#d9534f' : '#2b5797' }}>{data.stagnantLotsCount}</strong>
          </div>
        </div>
      )}

      {/* Lỗi nếu có */}
      {error && (
        <div
          style={{
            padding: '12px',
            marginBottom: '16px',
            backgroundColor: '#f8d7da',
            color: '#721c24',
            border: '1px solid #f5c6cb',
            borderRadius: '4px',
            fontSize: '13px',
          }}
        >
          {error}
        </div>
      )}

      {/* Bảng tiến độ 9 cột */}
      {data && <ChainProgressBoard data={data} loading={loading} />}
    </div>
  );
};
