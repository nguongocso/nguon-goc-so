import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { ChainProgressBoardData, ChainProgressItem, ChainProgressStageGroup } from '@/types/productionLot';

interface ChainProgressBoardProps {
  data: ChainProgressBoardData;
  loading?: boolean;
}

/**
 * Bảng theo dõi tiến độ chuỗi của từng lô (NCL-10-CN-013).
 * Thiết kế tối giản: Không modal, không icon, không màu sắc lòe loẹt.
 */
export const ChainProgressBoard: React.FC<ChainProgressBoardProps> = ({ data, loading }) => {
  const navigate = useNavigate();

  if (loading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center', color: '#666666' }}>
        Đang tải dữ liệu tiến độ chuỗi...
      </div>
    );
  }

  if (!data || !data.stages || data.stages.length === 0) {
    return (
      <div style={{ padding: '24px', textAlign: 'center', color: '#666666', border: '1px solid #e0e0e0', borderRadius: '4px' }}>
        Không có dữ liệu tiến độ chuỗi.
      </div>
    );
  }

  const handleCardClick = (item: ChainProgressItem) => {
    if (item.targetScreen) {
      navigate(item.targetScreen);
    }
  };

  return (
    <div style={{ overflowX: 'auto', paddingBottom: '16px' }}>
      <div
        style={{
          display: 'flex',
          gap: '12px',
          minWidth: '1600px', // Đảm bảo dàn đủ 9 cột
          alignItems: 'flex-start',
        }}
      >
        {data.stages.map((group: ChainProgressStageGroup) => (
          <div
            key={group.stage}
            style={{
              flex: '1 1 0',
              minWidth: '220px',
              backgroundColor: '#f8f9fa',
              border: '1px solid #e0e0e0',
              borderRadius: '4px',
              padding: '12px',
              display: 'flex',
              flexDirection: 'column',
              maxHeight: '75vh',
            }}
          >
            {/* Cột tiêu đề giai đoạn */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                borderBottom: '2px solid #cccccc',
                paddingBottom: '8px',
                marginBottom: '12px',
              }}
            >
              <span style={{ fontWeight: 600, fontSize: '14px', color: '#333333' }}>
                {group.stageName}
              </span>
              <span
                style={{
                  backgroundColor: '#e9ecef',
                  color: '#495057',
                  fontSize: '12px',
                  fontWeight: 600,
                  padding: '2px 8px',
                  borderRadius: '12px',
                }}
              >
                {group.count}
              </span>
            </div>

            {/* Danh sách các thẻ lô */}
            <div
              style={{
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
                gap: '10px',
                paddingRight: '2px',
              }}
            >
              {group.items.length === 0 ? (
                <div
                  style={{
                    fontSize: '12px',
                    color: '#888888',
                    fontStyle: 'italic',
                    textAlign: 'center',
                    padding: '16px 0',
                  }}
                >
                  Không có lô nào
                </div>
              ) : (
                group.items.map((item: ChainProgressItem) => (
                  <div
                    key={item.id}
                    onClick={() => handleCardClick(item)}
                    style={{
                      backgroundColor: '#ffffff',
                      border: item.isStagnant ? '1px solid #d9534f' : '1px solid #cccccc',
                      borderRadius: '4px',
                      padding: '10px 12px',
                      cursor: 'pointer',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                    }}
                  >
                    {/* Tên lô */}
                    <div style={{ fontWeight: 600, fontSize: '13px', color: '#111111', marginBottom: '4px' }}>
                      {item.name}
                    </div>

                    {/* Vùng trồng & Loại nông sản */}
                    <div style={{ fontSize: '12px', color: '#555555', marginBottom: '6px' }}>
                      {item.farmAreaName} • {item.productCategoryName}
                    </div>

                    {/* Badge tồn đọng hoặc số ngày lưu */}
                    <div style={{ display: 'flex', gap: '6px', alignItems: 'center', marginBottom: '8px', flexWrap: 'wrap' }}>
                      <span style={{ fontSize: '11px', color: '#666666' }}>
                        Thời gian: {item.daysInStage} ngày
                      </span>
                      {item.isStagnant && (
                        <span
                          style={{
                            fontSize: '11px',
                            color: '#d9534f',
                            border: '1px solid #d9534f',
                            backgroundColor: '#fff5f5',
                            padding: '1px 6px',
                            borderRadius: '3px',
                            fontWeight: 600,
                          }}
                        >
                          Tồn đọng ({item.daysInStage} ngày)
                        </span>
                      )}
                    </div>

                    {/* Việc cần làm tiếp theo */}
                    <div
                      style={{
                        fontSize: '11px',
                        color: '#2b5797',
                        borderTop: '1px dashed #e0e0e0',
                        paddingTop: '6px',
                        marginTop: '4px',
                      }}
                    >
                      <span style={{ fontWeight: 600 }}>Việc cần làm tiếp theo: </span>
                      <span>{item.nextActionRequired}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
