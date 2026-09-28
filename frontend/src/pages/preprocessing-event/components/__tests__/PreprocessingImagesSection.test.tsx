import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';

import { PreprocessingImagesSection } from '../PreprocessingImagesSection';

describe('PreprocessingImagesSection', () => {
  it('hiển thị nút chọn ảnh và giới hạn số lượng ảnh', () => {
    render(
      <PreprocessingImagesSection
        imageFiles={[]}
        imagePreviews={[]}
        isSubmitting={false}
        onImageChange={vi.fn()}
        onRemoveImage={vi.fn()}
      />,
    );

    expect(screen.getByText(/Hình ảnh thực địa/i)).toBeInTheDocument();
    expect(screen.getByText('0/5')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Chọn ảnh/i })).toBeEnabled();
  });

  it('hiển thị danh sách ảnh xem trước và gọi onRemoveImage khi xóa', () => {
    const handleRemove = vi.fn();
    const previews = ['blob:http://localhost/img-1', 'blob:http://localhost/img-2'];

    render(
      <PreprocessingImagesSection
        imageFiles={[new File([], 'img1.png'), new File([], 'img2.png')]}
        imagePreviews={previews}
        isSubmitting={false}
        onImageChange={vi.fn()}
        onRemoveImage={handleRemove}
      />,
    );

    expect(screen.getByText('2/5')).toBeInTheDocument();
    expect(screen.getByAltText('Ảnh sơ chế 1')).toBeInTheDocument();
    expect(screen.getByAltText('Ảnh sơ chế 2')).toBeInTheDocument();

    const deleteBtn = screen.getByRole('button', { name: 'Xóa ảnh 1' });
    fireEvent.click(deleteBtn);

    expect(handleRemove).toHaveBeenCalledWith(0);
  });

  it('vô hiệu hóa nút chọn ảnh khi đã đạt tối đa 5 ảnh', () => {
    const dummyFiles = Array.from({ length: 5 }, (_, i) => new File([], `img${i}.png`));
    const dummyPreviews = dummyFiles.map((_, i) => `blob:http://localhost/img-${i}`);

    render(
      <PreprocessingImagesSection
        imageFiles={dummyFiles}
        imagePreviews={dummyPreviews}
        isSubmitting={false}
        onImageChange={vi.fn()}
        onRemoveImage={vi.fn()}
      />,
    );

    expect(screen.getByText('5/5')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Chọn ảnh/i })).toBeDisabled();
  });
});
