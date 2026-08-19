import { useEffect, useState, useCallback } from 'react';
import { Eye, Download, Trash2, Upload, File, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { getAttachments, uploadAttachment, deleteAttachment, viewAttachment, downloadAttachment } from '@/api/attachmentApi';
import type { Attachment } from '@/types/attachment';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';

interface AttachmentManagerProps {
  logId: string;
  onUpdate?: (logId: string, action: 'upload' | 'delete') => void;
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
};

const formatDate = (dateStr: string) => {
  try {
    return format(new Date(dateStr), 'dd/MM/yyyy HH:mm', { locale: vi });
  } catch {
    return dateStr;
  }
};

const formatTypeLabel = (mimeType: string) => {
  if (mimeType.startsWith('image/')) return 'Ảnh';
  if (mimeType === 'application/pdf') return 'PDF';
  return mimeType;
};

const getTypeBadgeStyle = (mimeType: string): string => {
  if (mimeType.startsWith('image/')) return 'bg-purple-100 text-purple-700';
  if (mimeType === 'application/pdf') return 'bg-red-100 text-red-700';
  return 'bg-gray-100 text-gray-700';
};

export function AttachmentManager({ logId, onUpdate }: AttachmentManagerProps) {
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Upload dialog
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [description, setDescription] = useState('');
  const [isUploading, setIsUploading] = useState(false);

  // Preview dialog
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewSrc, setPreviewSrc] = useState('');
  const [previewType, setPreviewType] = useState('');
  const [previewFileName, setPreviewFileName] = useState('');

  // Delete confirmation
  const [deleteTarget, setDeleteTarget] = useState<Attachment | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Download action
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  // View action
  const [viewingId, setViewingId] = useState<string | null>(null);

  // Thumbnail blobs
  const [thumbnailUrls, setThumbnailUrls] = useState<Record<string, string>>({});
  const [loadingThumbnails, setLoadingThumbnails] = useState<Set<string>>(new Set());

  const loadAttachments = useCallback(async () => {
    if (!logId) return;
    try {
      setIsLoading(true);
      const data = await getAttachments(logId);
      setAttachments(data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách chứng từ');
    } finally {
      setIsLoading(false);
    }
  }, [logId]);

  useEffect(() => {
    if (logId) loadAttachments();
  }, [logId, loadAttachments]);

  // Load thumbnails for image attachments using blob URLs
  useEffect(() => {
    const imageAttachments = attachments.filter(a => a.fileType.startsWith('image/'));
    imageAttachments.forEach(async (att) => {
      if (thumbnailUrls[att.id] || loadingThumbnails.has(att.id)) return;
      try {
        setLoadingThumbnails(prev => new Set(prev).add(att.id));
        const { blob } = await viewAttachment(att.id);
        const url = URL.createObjectURL(blob);
        setThumbnailUrls(prev => ({ ...prev, [att.id]: url }));
      } catch {
        // Thumbnail load fails silently, will show fallback
      } finally {
        setLoadingThumbnails(prev => {
          const next = new Set(prev);
          next.delete(att.id);
          return next;
        });
      }
    });
  }, [attachments]);

  // Upload
  const handleUpload = async () => {
    if (!selectedFile) {
      toast.error('Vui lòng chọn file');
      return;
    }

    const validTypes = ['image/jpeg', 'image/png', 'application/pdf'];
    if (!validTypes.includes(selectedFile.type)) {
      toast.error('Chỉ hỗ trợ JPG, PNG, PDF');
      return;
    }

    if (selectedFile.size > 5 * 1024 * 1024) {
      toast.error('File vượt quá 5MB');
      return;
    }

    try {
      setIsUploading(true);
      await uploadAttachment(logId, selectedFile, description || undefined);
      toast.success('Ảnh đã được tải lên thành công.');
      setUploadDialogOpen(false);
      setSelectedFile(null);
      setDescription('');
      await loadAttachments();
      onUpdate?.(logId, 'upload');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Tải lên thất bại');
    } finally {
      setIsUploading(false);
    }
  };

  // View/Preview
  const handleView = async (att: Attachment) => {
    try {
      setViewingId(att.id);
      const { blob, contentType } = await viewAttachment(att.id);
      const url = URL.createObjectURL(blob);
      setPreviewSrc(url);
      setPreviewType(contentType);
      setPreviewFileName(att.fileName);
      setPreviewOpen(true);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể xem chứng từ');
    } finally {
      setViewingId(null);
    }
  };

  const handleClosePreview = () => {
    setPreviewOpen(false);
    if (previewSrc) {
      URL.revokeObjectURL(previewSrc);
    }
    setPreviewSrc('');
    setPreviewType('');
    setPreviewFileName('');
  };

  // Download
  const handleDownload = async (att: Attachment) => {
    try {
      setDownloadingId(att.id);
      await downloadAttachment(att.id, att.fileName);
      toast.success('Tải ảnh xuống thành công.');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Tải xuống thất bại');
    } finally {
      setDownloadingId(null);
    }
  };

  // Delete with confirmation
  const handleDeleteConfirm = (att: Attachment) => {
    setDeleteTarget(att);
  };

  const handleDeleteExecute = async () => {
    if (!deleteTarget) return;
    try {
      setIsDeleting(true);
      await deleteAttachment(deleteTarget.id);
      toast.success('Ảnh đã được xóa thành công.');
      setDeleteTarget(null);
      await loadAttachments();
      onUpdate?.(logId, 'delete');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Xóa thất bại');
    } finally {
      setIsDeleting(false);
    }
  };

  // Loading
  if (isLoading) {
    return (
      <div className="space-y-3">
        <div className="flex justify-between items-center">
          <span className="text-sm font-medium">Chứng từ đính kèm</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="rounded-lg border bg-card p-4 animate-pulse">
              <div className="h-24 bg-muted rounded mb-3" />
              <div className="h-4 bg-muted rounded w-3/4 mb-2" />
              <div className="h-3 bg-muted rounded w-1/2" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Header */}
      <div className="flex justify-between items-center">
        <span className="text-sm font-semibold">
          Chứng từ đính kèm ({attachments.length})
        </span>
        <Button variant="create" size="sm" onClick={() => setUploadDialogOpen(true)}>
          <Upload className="mr-1.5 h-3.5 w-3.5" /> Tải lên
        </Button>
      </div>

      {/* Empty state */}
      {attachments.length === 0 ? (
        <div className="py-8 text-center text-sm text-muted-foreground border rounded-lg bg-muted/20">
          <File className="mx-auto h-8 w-8 mb-2 opacity-40" />
          <p>Chưa có chứng từ</p>
          <p className="text-xs mt-1">Tải lên ảnh hoặc tài liệu PDF để làm chứng từ</p>
        </div>
      ) : (
        /* Attachment grid */
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
          {attachments.map((att) => {
            const isImage = att.fileType.startsWith('image/');

            return (
              <div
                key={att.id}
                className="rounded-lg border bg-card overflow-hidden shadow-sm hover:shadow-md transition-shadow"
              >
                {/* Thumbnail */}
                <div
                  className="relative h-32 bg-muted flex items-center justify-center cursor-pointer group"
                  onClick={() => handleView(att)}
                >
                  {isImage ? (
                    thumbnailUrls[att.id] ? (
                      <img
                        src={thumbnailUrls[att.id]}
                        alt={att.fileName}
                        className="h-full w-full object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="flex flex-col items-center text-muted-foreground">
                        <Eye className="h-10 w-10 mb-1 animate-pulse" />
                        <span className="text-xs">Đang tải...</span>
                      </div>
                    )
                  ) : (
                    <div className="flex flex-col items-center text-muted-foreground">
                      <FileText className="h-10 w-10 mb-1" />
                      <span className="text-xs">PDF</span>
                    </div>
                  )}
                  {/* Overlay on hover */}
                  <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
                    <Eye className="h-6 w-6 text-white opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                </div>

                {/* Info */}
                <div className="p-3 space-y-2">
                  <p className="text-sm font-medium truncate" title={att.fileName}>
                    {att.fileName}
                  </p>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${getTypeBadgeStyle(att.fileType)}`}>
                      {formatTypeLabel(att.fileType)}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {formatFileSize(att.fileSize)}
                    </span>
                  </div>
                  {att.description && (
                    <p className="text-xs text-muted-foreground truncate" title={att.description}>
                      {att.description}
                    </p>
                  )}
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span className="truncate max-w-[120px]" title={att.uploadedBy}>
                      {att.uploadedBy}
                    </span>
                    <span>{formatDate(att.uploadedAt)}</span>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center justify-end gap-1 pt-1 border-t">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => handleView(att)}
                      disabled={viewingId === att.id}
                      title="Xem"
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => handleDownload(att)}
                      disabled={downloadingId === att.id}
                      title="Tải xuống"
                    >
                      <Download className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-destructive hover:text-destructive"
                      onClick={() => handleDeleteConfirm(att)}
                      disabled={isDeleting}
                      title="Xóa"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Upload Dialog */}
      <Dialog open={uploadDialogOpen} onOpenChange={setUploadDialogOpen}>
  <DialogContent className="sm:max-w-md">
    <DialogHeader>
      <DialogTitle>Tải lên chứng từ</DialogTitle>
    </DialogHeader>
    <div className="space-y-4 py-2">
      {/* File upload */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="file" className="text-sm font-medium">
          Chọn file <span className="text-red-500">*</span>
        </Label>
        <Input
          id="file"
          type="file"
          accept=".jpg,.jpeg,.png,.pdf"
          onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
        />
        <p className="text-xs text-muted-foreground">Hỗ trợ JPG, PNG, PDF (≤5MB)</p>
      </div>

      {/* Description */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="desc" className="text-sm font-medium">
          Mô tả (tùy chọn)
        </Label>
        <Input
          id="desc"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Mô tả ngắn về chứng từ"
        />
      </div>
    </div>

    <div className="flex justify-end gap-2">
      <Button variant="outline" onClick={() => setUploadDialogOpen(false)} disabled={isUploading}>
        Hủy
      </Button>
      <Button variant="create" onClick={handleUpload} disabled={isUploading || !selectedFile}>
        {isUploading ? 'Đang tải lên...' : 'Tải lên'}
      </Button>
    </div>
  </DialogContent>
</Dialog>

      {/* Preview Dialog */}
      <Dialog open={previewOpen} onOpenChange={(open) => { if (!open) handleClosePreview(); }}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-auto">
          <DialogHeader>
            <DialogTitle className="truncate max-w-[80%]">{previewFileName}</DialogTitle>
          </DialogHeader>
          <div className="flex items-center justify-center min-h-[300px] bg-muted/30 rounded-lg">
            {previewType.startsWith('image/') ? (
              <img
                src={previewSrc}
                alt={previewFileName}
                className="max-w-full max-h-[70vh] object-contain rounded"
              />
            ) : previewType === 'application/pdf' ? (
              <iframe
                src={previewSrc}
                title={previewFileName}
                className="w-full h-[70vh] rounded"
              />
            ) : (
              <div className="flex flex-col items-center gap-3 py-12 text-muted-foreground">
                <FileText className="h-12 w-12" />
                <p className="text-sm">Không thể hiển thị loại file này</p>
                <Button variant="outline" size="sm" onClick={() => downloadAttachment(
                  previewSrc.replace(/^blob:/, '') ? '' : '',
                  previewFileName
                )}>
                  <Download className="mr-1.5 h-4 w-4" /> Tải xuống
                </Button>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={!!deleteTarget} onOpenChange={(open) => { if (!open) setDeleteTarget(null); }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa chứng từ?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa chứng từ "{deleteTarget?.fileName}"? Hành động này không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteExecute}
              disabled={isDeleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDeleting ? 'Đang xóa...' : 'Xóa'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}