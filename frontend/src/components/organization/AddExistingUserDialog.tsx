import { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { getAvailableUsers, addExistingUser } from '@/api/organizationApi';
import { toast } from 'sonner';
import { Loader2, Search, Maximize2, Minimize2, ChevronLeft, ChevronRight, X } from 'lucide-react';
import type { AvailableUser } from '@/types/organization';
import { getRoleLabel } from '@/config/roleAccess';

interface AddExistingUserDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    organizationId: string;
    onSuccess: () => void;
    availableRoles?: { id: number; code: string; name: string }[];
}

const PAGE_SIZE = 5; // số dòng mỗi trang

export function AddExistingUserDialog({
    open,
    onOpenChange,
    organizationId,
    onSuccess,
    availableRoles = [],
}: AddExistingUserDialogProps) {
    const [users, setUsers] = useState<AvailableUser[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedRole, setSelectedRole] = useState<Record<string, number>>({});
    const [submitting, setSubmitting] = useState<string | null>(null);

    // Search & pagination state
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(1);

    // Resize / maximize state
    const [isMaximized, setIsMaximized] = useState(false);
    const [dialogSize, setDialogSize] = useState<{ width: number; height: number } | null>(null);
    const resizeRef = useRef<{ startX: number; startY: number; startWidth: number; startHeight: number } | null>(null);
    const dialogRef = useRef<HTMLDivElement>(null);

    // Load users when dialog opens
    const loadAvailableUsers = useCallback(async () => {
        try {
            setLoading(true);
            const response = await getAvailableUsers(organizationId);
            setUsers(response);
        } catch (error) {
            toast.error('Không thể tải danh sách user có sẵn');
        } finally {
            setLoading(false);
        }
    }, [organizationId]);

    useEffect(() => {
        if (open) {
            loadAvailableUsers();
            // Reset pagination and search when dialog opens
            setSearchTerm('');
            setCurrentPage(1);
            // Reset size/maximize
            setIsMaximized(false);
            setDialogSize(null);
        }
    }, [open, loadAvailableUsers]);

    // Filter users by search term (case-insensitive)
    const filteredUsers = useMemo(() => {
        if (!searchTerm.trim()) return users;
        const term = searchTerm.toLowerCase();
        return users.filter(
            (u) =>
                u.username.toLowerCase().includes(term) ||
                u.fullName.toLowerCase().includes(term) ||
                (u.email && u.email.toLowerCase().includes(term))
        );
    }, [users, searchTerm]);

    // Pagination logic
    const totalPages = Math.ceil(filteredUsers.length / PAGE_SIZE);
    const safePage = Math.min(currentPage, Math.max(1, totalPages));
    const paginatedUsers = useMemo(() => {
        const start = (safePage - 1) * PAGE_SIZE;
        return filteredUsers.slice(start, start + PAGE_SIZE);
    }, [filteredUsers, safePage]);

    const goToPage = (page: number) => {
        setCurrentPage(Math.max(1, Math.min(page, totalPages || 1)));
    };

    // Reset page when search changes
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm]);

    // Handle add user
    const handleAddUser = async (userId: string) => {
        try {
            setSubmitting(userId);
            const roleId = selectedRole[userId] || undefined;
            await addExistingUser(organizationId, { userId, roleId });
            toast.success('Thêm user thành công');
            onSuccess();
            setUsers((prev) => prev.filter((u) => u.userId !== userId));
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Thêm user thất bại');
        } finally {
            setSubmitting(null);
        }
    };

    // Resize handlers
    const startResize = useCallback(
        (e: React.MouseEvent<HTMLDivElement>) => {
            if (!dialogRef.current) return;
            const rect = dialogRef.current.getBoundingClientRect();
            resizeRef.current = {
                startX: e.clientX,
                startY: e.clientY,
                startWidth: rect.width,
                startHeight: rect.height,
            };
            document.addEventListener('mousemove', onResize);
            document.addEventListener('mouseup', stopResize);
            e.preventDefault();
        },
        []
    );

    const onResize = useCallback((e: MouseEvent) => {
        if (!resizeRef.current) return;
        const { startX, startY, startWidth, startHeight } = resizeRef.current;
        const newWidth = Math.max(400, startWidth + (e.clientX - startX));
        const newHeight = Math.max(300, startHeight + (e.clientY - startY));
        setDialogSize({ width: newWidth, height: newHeight });
    }, []);

    const stopResize = useCallback(() => {
        document.removeEventListener('mousemove', onResize);
        document.removeEventListener('mouseup', stopResize);
        resizeRef.current = null;
    }, [onResize]);

    // Clean up listeners on unmount
    useEffect(() => {
        return () => {
            document.removeEventListener('mousemove', onResize);
            document.removeEventListener('mouseup', stopResize);
        };
    }, [onResize, stopResize]);

    const toggleMaximize = () => {
        if (isMaximized) {
            setIsMaximized(false);
            setDialogSize(null);
        } else {
            setIsMaximized(true);
            setDialogSize(null); // maximize uses viewport units
        }
    };

    // Dynamic style for dialog content
    const dialogStyle: React.CSSProperties = {
        maxWidth: 'none',
        width: '984px',
        maxHeight: '90vh',
    };

    if (isMaximized) {
        dialogStyle.width = '96vw';
        dialogStyle.height = '96vh';
        dialogStyle.maxWidth = '96vw';
    } else if (dialogSize) {
        dialogStyle.width = dialogSize.width;
        dialogStyle.height = dialogSize.height;
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                ref={dialogRef}
                className="flex flex-col"
                style={dialogStyle}
                showCloseButton={false}
            >
                {/* Custom header with title, maximize and close buttons */}
                <DialogHeader className="flex flex-row items-center justify-between">
                    <DialogTitle>Thêm tài khoản đã tồn tại</DialogTitle>
                    <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" onClick={toggleMaximize} className="h-8 w-8">
                            {isMaximized ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
                        </Button>
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => onOpenChange(false)}
                            className="h-8 w-8"
                        >
                            <X className="h-4 w-4" />
                        </Button>
                    </div>
                </DialogHeader>

                {/* Search */}
                <div className="relative mb-4">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                        placeholder="Tìm kiếm theo tên, email, username..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-9"
                    />
                </div>

                {loading ? (
                    <div className="flex justify-center py-8">
                        <Loader2 className="h-8 w-8 animate-spin" />
                    </div>
                ) : filteredUsers.length === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                        {users.length === 0 ? (
                            <>
                                Không có user nào có sẵn để thêm.
                                <br />
                                <span className="text-sm">
                                    (Các user đã có trong tổ chức cùng loại nhưng chưa có trong tổ chức này)
                                </span>
                            </>
                        ) : (
                            'Không tìm thấy kết quả phù hợp.'
                        )}
                    </div>
                ) : (
                    <>
                        <div className="flex-1 overflow-auto">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="w-[140px]">Tài khoản</TableHead>
                                        <TableHead className="w-[160px]">Họ tên</TableHead>
                                        <TableHead className="w-[200px]">Email</TableHead>
                                        <TableHead className="w-[150px]">Vai trò hiện tại</TableHead>
                                        <TableHead className="w-[220px]">Chọn vai trò mới</TableHead>
                                        <TableHead className="w-[100px]">Thao tác</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {paginatedUsers.map((user) => (
                                        <TableRow key={user.userId}>
                                            <TableCell>{user.username}</TableCell>
                                            <TableCell>{user.fullName}</TableCell>
                                            <TableCell className="truncate max-w-[200px]">{user.email || ''}</TableCell>
                                            <TableCell>
                                                <Badge variant="outline">
                                                    {getRoleLabel(user.currentRoleCode) || user.currentRoleName}
                                                </Badge>
                                            </TableCell>
                                            <TableCell>
                                                {availableRoles.length > 0 ? (
                                                    <Select
                                                        value={
                                                            selectedRole[user.userId]
                                                                ? String(selectedRole[user.userId])
                                                                : ''
                                                        }
                                                        onValueChange={(val) => {
                                                            setSelectedRole((prev) => ({
                                                                ...prev,
                                                                [user.userId]: Number(val),
                                                            }));
                                                        }}
                                                    >
                                                        <SelectTrigger className="w-full min-w-[180px]">
                                                            <SelectValue placeholder="Giữ nguyên">
                                                                {(() => {
                                                                    const roleId = selectedRole[user.userId];
                                                                    if (!roleId) return 'Giữ nguyên';
                                                                    const role = availableRoles.find(
                                                                        (r) => r.id === roleId
                                                                    );
                                                                    return role
                                                                        ? getRoleLabel(role.code)
                                                                        : String(roleId);
                                                                })()}
                                                            </SelectValue>
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            {availableRoles.map((role) => (
                                                                <SelectItem key={role.id} value={String(role.id)}>
                                                                    {getRoleLabel(role.code) || role.name}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                ) : (
                                                    <span className="text-xs text-muted-foreground">
                                                        Không thể đổi role
                                                    </span>
                                                )}
                                            </TableCell>
                                            <TableCell>
                                                <Button
                                                    size="sm"
                                                    onClick={() => handleAddUser(user.userId)}
                                                    disabled={submitting === user.userId}
                                                >
                                                    {submitting === user.userId ? (
                                                        <Loader2 className="h-4 w-4 animate-spin" />
                                                    ) : (
                                                        'Thêm'
                                                    )}
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="flex items-center justify-between pt-4">
                                <div className="text-sm text-muted-foreground">
                                    {filteredUsers.length} kết quả, trang {safePage}/{totalPages}
                                </div>
                                <div className="flex items-center gap-2">
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => goToPage(safePage - 1)}
                                        disabled={safePage <= 1}
                                    >
                                        <ChevronLeft className="h-4 w-4 mr-1" /> Trước
                                    </Button>
                                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                                        let start = Math.max(1, safePage - 2);
                                        let end = Math.min(totalPages, start + 4);
                                        if (end - start < 4) start = Math.max(1, end - 4);
                                        const page = start + i;
                                        if (page > end) return null;
                                        return (
                                            <Button
                                                key={page}
                                                variant={page === safePage ? 'default' : 'outline'}
                                                size="sm"
                                                onClick={() => goToPage(page)}
                                                className="h-8 w-8 p-0"
                                            >
                                                {page}
                                            </Button>
                                        );
                                    })}
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => goToPage(safePage + 1)}
                                        disabled={safePage >= totalPages}
                                    >
                                        Sau <ChevronRight className="h-4 w-4 ml-1" />
                                    </Button>
                                </div>
                            </div>
                        )}
                    </>
                )}

                {/* Resize handle (hidden when maximized) */}
                {!isMaximized && (
                    <div
                        className="resize-handle absolute bottom-0 right-0 w-6 h-6 cursor-nwse-resize flex items-center justify-center text-muted-foreground hover:text-foreground"
                        onMouseDown={startResize}
                    >
                        <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                            <path d="M0 12 L12 0 L12 4 L4 12 Z M0 8 L8 0 L12 0 L12 2 L2 12 L0 12 Z" />
                        </svg>
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
}