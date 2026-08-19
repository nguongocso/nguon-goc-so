import React, { useState, useEffect, useCallback } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import axios from 'axios';

import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

import { Badge } from '@/components/ui/badge';

import {
  Loader2,
  Upload,
  Download,
  FileSpreadsheet,
  CheckCircle2,
  XCircle,
  AlertTriangle,
} from 'lucide-react';

import {
  importProductionLots,
  downloadImportTemplate,
  getFarmAreaOptions,
  getProductCategoryOptions,
} from '@/api/productionLotApi';

import { getOrganizations } from '@/api/organizationApi';

import {
  importProductionLotSchema,
  type ImportProductionLotFormValues,
} from '@/utils/validators';

import type {
  ProductionLotImportResultResponse,
} from '@/types/productionLotImport';

import type {
  Organization,
} from '@/types/organization';

import type {
  FarmAreaOption,
  ProductCategoryOption,
} from '@/types/productionLot';

import { useAuth } from '@/hooks/useAuth';

// =========================================================
// ERROR MESSAGE
// =========================================================

const ERROR_MESSAGES: Record<number, string> = {
  400: 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại tệp Excel.',
  401: 'Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  403: 'Bạn không có quyền thực hiện chức năng này.',
  404: 'API không tồn tại. Vui lòng liên hệ quản trị viên.',
  409: 'Dữ liệu đã tồn tại hoặc vi phạm ràng buộc.',
  413: 'Tệp quá lớn. Vui lòng giảm kích thước tệp hoặc tách thành nhiều tệp nhỏ.',
  422: 'Dữ liệu trong tệp không hợp lệ. Vui lòng kiểm tra lại.',
  500: 'Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.',
};

// =========================================================
// ERROR HANDLER
// =========================================================

const getErrorMessage = (error: unknown): string => {
  if (!axios.isAxiosError(error)) {
    if (error instanceof Error) {
      return error.message;
    }

    return 'Đã xảy ra lỗi không xác định.';
  }

  const backendMessage = error.response?.data?.message;

  if (
    backendMessage &&
    typeof backendMessage === 'string' &&
    backendMessage.trim() !== ''
  ) {
    return backendMessage;
  }

  const status = error.response?.status;

  if (status && ERROR_MESSAGES[status]) {
    return ERROR_MESSAGES[status];
  }

  if (
    error.code === 'ERR_NETWORK' ||
    error.code === 'ECONNABORTED'
  ) {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
  }

  return 'Đã xảy ra lỗi khi nhập dữ liệu. Vui lòng thử lại.';
};

// =========================================================
// PROPS
// =========================================================

interface ImportProductionLotFormProps {
  onSuccess?: () => void;
}

// =========================================================
// COMPONENT
// =========================================================

export const ImportProductionLotForm: React.FC<
  ImportProductionLotFormProps
> = ({ onSuccess }) => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // -------------------------------------------------------
  // STATE
  // -------------------------------------------------------

  const [submitting, setSubmitting] = useState(false);

  const [organizations, setOrganizations] = useState<Organization[]>(
    [],
  );

  const [loadingOrgs, setLoadingOrgs] = useState(false);

  const [farmAreas, setFarmAreas] = useState<FarmAreaOption[]>(
    [],
  );

  const [productCategories, setProductCategories] = useState<
    ProductCategoryOption[]
  >([]);

  const [loadingOptions, setLoadingOptions] = useState(false);

  const [result, setResult] =
    useState<ProductionLotImportResultResponse | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);

  const [uploadProgress, setUploadProgress] = useState(0);

  const isAdmin = user?.roleCode === 'VT-01';

  // -------------------------------------------------------
  // FORM
  // -------------------------------------------------------

  const {
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
    reset,
  } = useForm<ImportProductionLotFormValues>({
    resolver: zodResolver(importProductionLotSchema),
    defaultValues: {
      file: undefined,
      organizationId: '',
      productCategoryId: '',
      farmAreaId: '',
    },
  });

  // -------------------------------------------------------
  // WATCH
  // -------------------------------------------------------

  const selectedFile = watch('file');

  const selectedProductCategoryId = watch(
    'productCategoryId',
  );

  const selectedFarmAreaId = watch(
    'farmAreaId',
  );

  // -------------------------------------------------------
  // LOAD ORGANIZATIONS
  // -------------------------------------------------------

  useEffect(() => {
    if (!isAdmin) {
      return;
    }

    setLoadingOrgs(true);

    getOrganizations()
      .then((data) => {
        setOrganizations(data);
      })
      .catch((error) => {
        toast.error(getErrorMessage(error));
      })
      .finally(() => {
        setLoadingOrgs(false);
      });
  }, [isAdmin]);

  // -------------------------------------------------------
  // LOAD FARM AREAS + PRODUCT CATEGORIES
  // -------------------------------------------------------

  useEffect(() => {
    const loadOptions = async () => {
      setLoadingOptions(true);

      try {
        const [
          farmAreaData,
          productCategoryData,
        ] = await Promise.all([
          getFarmAreaOptions(),
          getProductCategoryOptions(),
        ]);

        setFarmAreas(farmAreaData);
        setProductCategories(productCategoryData);
      } catch (error) {
        toast.error(
          getErrorMessage(error),
        );
      } finally {
        setLoadingOptions(false);
      }
    };

    loadOptions();
  }, []);

  // -------------------------------------------------------
  // FILE CHANGE
  // -------------------------------------------------------

  const handleFileChange = (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = e.target.files?.[0];

    if (!file) {
      return;
    }

    setValue('file', file, {
      shouldValidate: true,
      shouldDirty: true,
    });

    setUploadProgress(0);
    setResult(null);
  };

  // -------------------------------------------------------
  // DOWNLOAD TEMPLATE
  // -------------------------------------------------------

  const handleDownloadTemplate = useCallback(
    async () => {
      if (!selectedProductCategoryId) {
        toast.error(
          'Vui lòng chọn loại nông sản trước.',
        );

        return;
      }

      if (!selectedFarmAreaId) {
        toast.error(
          'Vui lòng chọn vùng trồng trước.',
        );

        return;
      }

      try {
        await downloadImportTemplate(
          selectedProductCategoryId,
          selectedFarmAreaId,
        );

        toast.success(
          'Đã tải file Excel mẫu thành công.',
        );
      } catch (error) {
        toast.error(
          getErrorMessage(error),
        );
      }
    },
    [
      selectedProductCategoryId,
      selectedFarmAreaId,
    ],
  );

  // -------------------------------------------------------
  // SUBMIT
  // -------------------------------------------------------

  const onSubmit = async (
    data: ImportProductionLotFormValues,
  ) => {
    if (!data.file) {
      toast.error(
        'Vui lòng chọn file Excel.',
      );

      return;
    }

    setSubmitting(true);
    setUploadProgress(10);

    let progressTimer:
      ReturnType<typeof setInterval> | null =
      null;

    try {
      progressTimer = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 80) {
            if (progressTimer) {
              clearInterval(
                progressTimer,
              );

              progressTimer = null;
            }

            return prev;
          }

          return prev + 10;
        });
      }, 300);

      const importResult =
        await importProductionLots(
          data.file,
          data.organizationId ||
          undefined,
        );

      if (progressTimer) {
        clearInterval(
          progressTimer,
        );

        progressTimer = null;
      }

      setUploadProgress(100);

      setResult(importResult);
      setDialogOpen(true);

      reset();

      onSuccess?.();

      if (
        importResult.status ===
        'SUCCESS'
      ) {
        toast.success(
          'Nhập dữ liệu lô sản xuất thành công.',
        );
      } else if (
        importResult.status ===
        'PARTIAL_SUCCESS'
      ) {
        toast.warning(
          'Nhập dữ liệu hoàn tất nhưng có một số dòng lỗi.',
        );
      } else {
        toast.error(
          'Không có dữ liệu nào được nhập thành công.',
        );
      }
    } catch (error: unknown) {
      if (progressTimer) {
        clearInterval(
          progressTimer,
        );

        progressTimer = null;
      }

      toast.error(
        getErrorMessage(error),
      );
    } finally {
      setSubmitting(false);

      setTimeout(() => {
        setUploadProgress(0);
      }, 500);
    }
  };

  // -------------------------------------------------------
  // CLOSE RESULT DIALOG
  // -------------------------------------------------------

  const handleCloseDialog = () => {
    setDialogOpen(false);

    if (
      result?.status === 'SUCCESS' ||
      result?.status ===
      'PARTIAL_SUCCESS'
    ) {
      navigate('/production-lots');
    }
  };

  // -------------------------------------------------------
  // STATUS CONFIG
  // -------------------------------------------------------

  const getStatusConfig = (
    status: ProductionLotImportResultResponse['status'],
  ) => {
    switch (status) {
      case 'SUCCESS':
        return {
          badge: (
            <Badge variant="default">
              Thành công
            </Badge>
          ),
          icon: (
            <CheckCircle2 className="h-5 w-5 text-emerald-600" />
          ),
          label: 'Thành công',
        };

      case 'PARTIAL_SUCCESS':
        return {
          badge: (
            <Badge variant="secondary">
              Thành công một phần
            </Badge>
          ),
          icon: (
            <AlertTriangle className="h-5 w-5 text-yellow-600" />
          ),
          label: 'Thành công một phần',
        };

      case 'FAILED':
        return {
          badge: (
            <Badge variant="destructive">
              Thất bại
            </Badge>
          ),
          icon: (
            <XCircle className="h-5 w-5 text-red-600" />
          ),
          label: 'Thất bại',
        };

      default:
        return {
          badge: (
            <Badge variant="outline">
              {status}
            </Badge>
          ),
          icon: null,
          label: status,
        };
    }
  };

  const statusConfig = result
    ? getStatusConfig(result.status)
    : null;

  // -------------------------------------------------------
  // RENDER
  // -------------------------------------------------------

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>
            Nhập dữ liệu lô sản xuất hàng loạt
          </CardTitle>

          <CardDescription>
            Tải lên file Excel (.xlsx) theo
            mẫu chuẩn để nhập danh sách lô
            sản xuất.
          </CardDescription>
        </CardHeader>

        <form
          onSubmit={handleSubmit(
            onSubmit,
          )}
        >
          <CardContent className="space-y-6">

            {/* =========================
                            TỔ CHỨC
                        ========================== */}

            {isAdmin && (
              <div className="space-y-2">
                <Label>
                  Tổ chức
                </Label>

                <Controller
                  name="organizationId"
                  control={control}
                  render={({
                    field,
                  }) => (
                    <Select
                      value={
                        field.value ||
                        ''
                      }
                      onValueChange={
                        field.onChange
                      }
                      disabled={
                        loadingOrgs ||
                        submitting
                      }
                    >
                      <SelectTrigger>
                        <SelectValue
                          placeholder={
                            loadingOrgs
                              ? 'Đang tải tổ chức...'
                              : 'Chọn tổ chức'
                          }
                        />
                      </SelectTrigger>

                      <SelectContent>
                        <SelectItem value="">
                          Tổ chức của tôi
                        </SelectItem>

                        {organizations.map(
                          (
                            org,
                          ) => (
                            <SelectItem
                              key={
                                org.id
                              }
                              value={
                                org.id
                              }
                            >
                              {
                                org.name
                              }{' '}
                              (
                              {
                                org.code
                              }
                              )
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  )}
                />

                {errors.organizationId && (
                  <p className="text-sm text-red-500">
                    {
                      errors
                        .organizationId
                        .message
                    }
                  </p>
                )}
              </div>
            )}

            {/* =========================
                            LOẠI NÔNG SẢN
                        ========================== */}

            <div className="space-y-2">
              <Label>
                Loại nông sản
              </Label>

              <Controller
                name="productCategoryId"
                control={control}
                render={({ field }) => {
                  const selectedCategory = productCategories.find(
                    (category) => category.id === field.value
                  );

                  return (
                    <Select
                      value={field.value || ''}
                      onValueChange={field.onChange}
                      disabled={loadingOptions || submitting}
                    >
                      <SelectTrigger>
                        <SelectValue
                          placeholder={
                            loadingOptions
                              ? 'Đang tải...'
                              : 'Chọn loại nông sản'
                          }
                        >
                          {selectedCategory?.name}
                        </SelectValue>
                      </SelectTrigger>

                      <SelectContent>
                        {productCategories.map((category) => (
                          <SelectItem
                            key={category.id}
                            value={category.id}
                          >
                            {category.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  );
                }}
              />

              {errors.productCategoryId && (
                <p className="text-sm text-red-500">
                  {
                    errors
                      .productCategoryId
                      .message
                  }
                </p>
              )}
            </div>

            {/* =========================
                            VÙNG TRỒNG
                        ========================== */}

            <div className="space-y-2">
              <Label>
                Vùng trồng
              </Label>

              <Controller
                name="farmAreaId"
                control={control}
                render={({ field }) => {
                  const selectedFarmArea = farmAreas.find(
                    (farmArea) => farmArea.id === field.value
                  );

                  return (
                    <Select
                      value={field.value || ''}
                      onValueChange={field.onChange}
                      disabled={loadingOptions || submitting}
                    >
                      <SelectTrigger>
                        <SelectValue
                          placeholder={
                            loadingOptions
                              ? 'Đang tải...'
                              : 'Chọn vùng trồng'
                          }
                        >
                          {selectedFarmArea?.name}
                        </SelectValue>
                      </SelectTrigger>

                      <SelectContent>
                        {farmAreas.map((farmArea) => (
                          <SelectItem
                            key={farmArea.id}
                            value={farmArea.id}
                          >
                            {farmArea.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  );
                }}
              />

              {errors.farmAreaId && (
                <p className="text-sm text-red-500">
                  {
                    errors
                      .farmAreaId
                      .message
                  }
                </p>
              )}
            </div>

            {/* =========================
                            FILE EXCEL
                        ========================== */}

            <div className="space-y-2">
              <Label htmlFor="file">
                Tệp dữ liệu *
              </Label>

              <div className="flex flex-wrap items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    document
                      .getElementById(
                        'production-lot-import-file',
                      )
                      ?.click()
                  }
                  disabled={
                    submitting
                  }
                >
                  <FileSpreadsheet className="mr-1 h-4 w-4" />
                  Chọn file Excel
                </Button>

                <input
                  id="production-lot-import-file"
                  type="file"
                  accept=".xlsx"
                  className="hidden"
                  onChange={
                    handleFileChange
                  }
                  disabled={
                    submitting
                  }
                />

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={
                    handleDownloadTemplate
                  }
                  disabled={
                    submitting ||
                    loadingOptions ||
                    !selectedProductCategoryId ||
                    !selectedFarmAreaId
                  }
                >
                  <Download className="mr-1 h-4 w-4" />
                  Tải file Excel mẫu
                </Button>
              </div>

              {selectedFile && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <FileSpreadsheet className="h-4 w-4" />

                  <span className="font-medium">
                    {
                      selectedFile.name
                    }
                  </span>

                  <span className="text-xs">
                    (
                    {(
                      selectedFile.size /
                      1024
                    ).toFixed(
                      1,
                    )}{' '}
                    KB)
                  </span>
                </div>
              )}

              {errors.file && (
                <p className="text-sm text-red-500">
                  {
                    errors
                      .file
                      .message
                  }
                </p>
              )}
            </div>

            {/* =========================
                            PROGRESS
                        ========================== */}

            {submitting &&
              uploadProgress >
              0 && (
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">
                      Đang tải lên và
                      xử lý...
                    </span>

                    <span className="font-medium">
                      {
                        uploadProgress
                      }
                      %
                    </span>
                  </div>

                  <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
                    <div
                      className="h-2 rounded-full bg-emerald-500 transition-all duration-300 ease-out"
                      style={{
                        width: `${uploadProgress}%`,
                      }}
                    />
                  </div>
                </div>
              )}

            {/* =========================
                            HƯỚNG DẪN
                        ========================== */}

            <div className="rounded-lg bg-muted p-4 text-sm">
              <p className="font-medium">
                Yêu cầu file Excel:
              </p>

              <ul className="mt-2 list-inside list-disc space-y-1 text-muted-foreground">
                <li>
                  Định dạng:{' '}
                  <strong>
                    .xlsx
                  </strong>
                </li>

                <li>
                  Sử dụng file Excel
                  mẫu được cung cấp
                  bởi hệ thống.
                </li>

                <li>
                  Loại nông sản và vùng
                  trồng được chọn trước
                  khi tải mẫu.
                </li>

                <li>
                  Các trường: ten_lo, ma_loai_nong_san, ma_vung_trong, san_luong_du_kien là {' '}
                  <strong>
                  bắt buộc.
                  </strong>
                  
                </li>

                <li>
                  san_luong_du_kien có đơn vị là {''}
                  <strong>
                  kg
                  </strong>
                </li>

                <li>
                  Ngày tháng theo định
                  dạng:{' '}
                  <strong>
                    dd/MM/yyyy
                  </strong>
                </li>

                <li>
                  Các dòng không hợp lệ
                  sẽ được trả về chi
                  tiết lỗi theo số dòng.
                </li>

                <li>
                  Nếu một số dòng hợp lệ
                  và một số dòng lỗi,
                  các dòng hợp lệ vẫn
                  được lưu.
                </li>
              </ul>
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-2">
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() =>
                navigate(
                  '/production-lots',
                )
              }
              disabled={submitting}
            >
              Hủy
            </Button>

            <Button
              type="submit"
              size="sm"
              disabled={
                submitting ||
                !selectedFile
              }
            >
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                <>
                  <Upload className="mr-2 h-4 w-4" />
                  Nhập dữ liệu
                </>
              )}
            </Button>
          </CardFooter>
        </form>
      </Card>

      {/* =========================
                RESULT DIALOG
            ========================== */}

      <Dialog
        open={dialogOpen}
        onOpenChange={
          setDialogOpen
        }
      >
        <DialogContent className="max-h-[85vh] max-w-lg overflow-y-auto md:max-w-2xl lg:max-w-3xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {statusConfig?.icon}
              Kết quả nhập dữ liệu
            </DialogTitle>
          </DialogHeader>

          {result && (
            <div className="space-y-4">

              {/* FILE */}

              {result.fileName && (
                <div className="text-sm text-muted-foreground">
                  <span className="font-medium">
                    Tệp:
                  </span>{' '}
                  {
                    result.fileName
                  }
                </div>
              )}

              {/* SUMMARY */}

              <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <div className="rounded-lg bg-muted p-3 text-center">
                  <p className="text-2xl font-bold">
                    {
                      result.totalRows
                    }
                  </p>

                  <p className="text-xs text-muted-foreground">
                    Tổng dòng
                  </p>
                </div>

                <div className="rounded-lg bg-emerald-50 p-3 text-center">
                  <p className="text-2xl font-bold text-emerald-600">
                    {
                      result.successCount
                    }
                  </p>

                  <p className="text-xs text-muted-foreground">
                    Thành công
                  </p>
                </div>

                <div className="rounded-lg bg-red-50 p-3 text-center">
                  <p className="text-2xl font-bold text-red-600">
                    {
                      result.failedCount
                    }
                  </p>

                  <p className="text-xs text-muted-foreground">
                    Thất bại
                  </p>
                </div>

                <div className="flex flex-col items-center justify-center rounded-lg bg-blue-50 p-3 text-center">
                  {
                    statusConfig?.badge
                  }

                  <p className="mt-1 text-xs text-muted-foreground">
                    Trạng thái
                  </p>
                </div>
              </div>

              {/* ERRORS */}

              {result.errors &&
                result.errors.length >
                0 && (
                  <div>
                    <h4 className="mb-2 font-medium">
                      Chi tiết lỗi
                      theo dòng
                    </h4>

                    <div className="overflow-x-auto rounded-lg border">
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead className="w-20">
                              Dòng
                            </TableHead>

                            <TableHead>
                              Lý do
                            </TableHead>
                          </TableRow>
                        </TableHeader>

                        <TableBody>
                          {result.errors.map(
                            (
                              err,
                              index,
                            ) => (
                              <TableRow
                                key={`${err.rowNumber}-${index}`}
                              >
                                <TableCell className="font-mono">
                                  {
                                    err.rowNumber
                                  }
                                </TableCell>

                                <TableCell className="text-red-600">
                                  {
                                    err.reason
                                  }
                                </TableCell>
                              </TableRow>
                            ),
                          )}
                        </TableBody>
                      </Table>
                    </div>
                  </div>
                )}

              {/* ALL SUCCESS */}

              {result.failedCount ===
                0 && (
                  <div className="py-4 text-center text-emerald-600">
                    <CheckCircle2 className="mx-auto mb-2 h-12 w-12" />

                    <p className="font-medium">
                      Tất cả{' '}
                      {
                        result.successCount
                      }{' '}
                      dòng đều được
                      nhập thành
                      công!
                    </p>
                  </div>
                )}

              {/* ALL FAILED */}

              {result.successCount ===
                0 &&
                result.failedCount >
                0 && (
                  <div className="py-4 text-center text-red-600">
                    <XCircle className="mx-auto mb-2 h-12 w-12" />

                    <p className="font-medium">
                      Không có
                      dòng nào
                      được nhập
                      thành công.
                    </p>

                    <p className="text-sm">
                      Vui lòng sửa
                      lỗi và thử
                      lại.
                    </p>
                  </div>
                )}

              {/* PARTIAL */}

              {result.successCount >
                0 &&
                result.failedCount >
                0 && (
                  <div className="py-4 text-center text-yellow-600">
                    <AlertTriangle className="mx-auto mb-2 h-12 w-12" />

                    <p className="font-medium">
                      {
                        result.successCount
                      }{' '}
                      dòng thành
                      công,{' '}
                      {
                        result.failedCount
                      }{' '}
                      dòng thất
                      bại.
                    </p>

                    <p className="text-sm">
                      Các dòng thành
                      công đã được
                      lưu. Vui lòng
                      sửa các dòng
                      lỗi và nhập
                      lại.
                    </p>
                  </div>
                )}

              {/* IMPORT TIME */}

              {result.importedAt && (
                <div className="text-right text-xs text-muted-foreground">
                  Hoàn tất:{' '}
                  {new Date(
                    result.importedAt,
                  ).toLocaleString(
                    'vi-VN',
                  )}
                </div>
              )}

              <DialogFooter>
                <Button
                  variant="view"
                  onClick={
                    handleCloseDialog
                  }
                >
                  {result.status ===
                    'SUCCESS'
                    ? 'Xem danh sách lô'
                    : 'Đóng'}
                </Button>
              </DialogFooter>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
};