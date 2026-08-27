import {useEffect, useState, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {PlusCircle, RefreshCw, Search} from 'lucide-react';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Card, CardContent} from '@/components/ui/card';
import {Badge} from '@/components/ui/badge';
import {HelpButton} from '@/components/help/HelpButton';
import {Pagination} from '@/components/common/Pagination';
import {getOrganizations} from '@/api/organizationApi';
import {type Organization} from '@/types/organization';
import {ORGANIZATION_TYPES} from '@/utils/constants';
import {usePermission} from '@/hooks/usePermission';
import {ROLE_ACCESS} from '@/config/roleAccess';

const PAGE_SIZE = 10;

export function OrganizationListPage() {
    const navigate = useNavigate();
    const [organizations, setOrganizations] = useState<Organization[]>([]);
    const [loading, setLoading] = useState(true);

    const canCreate = usePermission(ROLE_ACCESS.organizationCreate);

    // Tìm kiếm & phân trang (client-side)
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);

    const fetchOrganizations = async () => {
        try {
            setLoading(true);
            const data = await getOrganizations();

            const mappedData: Organization[] = data.map((item: any) => ({
                id: item.organizationID,
                name: item.organizationName,
                code: item.organizationCode,
                type: item.organizationType,
                status: item.status,
                createdAt: item.createdAt,
                updatedAt: item.updatedAt,
            }));

            setOrganizations(mappedData);
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Không thể tải danh sách tổ chức');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchOrganizations();
    }, []);

    // Lọc theo từ khóa tìm kiếm (client-side)
    const filtered = useMemo(() => {
        if (!search.trim()) return organizations;
        const q = search.toLowerCase().trim();
        return organizations.filter(
            (org) =>
                org.name.toLowerCase().includes(q) ||
                org.code.toLowerCase().includes(q),
        );
    }, [organizations, search]);

    const totalPages = Math.ceil(filtered.length / PAGE_SIZE);

    // Phân trang client-side
    const paginated = useMemo(() => {
        const start = page * PAGE_SIZE;
        return filtered.slice(start, start + PAGE_SIZE);
    }, [filtered, page]);

    const getStatusBadge = (status: string) => {
        const variants: Record<string, 'default' | 'destructive' | 'secondary'> = {
            ACTIVE: 'default',
            INACTIVE: 'destructive',
        };
        const labels: Record<string, string> = {
            ACTIVE: 'Đang hoạt động',
            INACTIVE: 'Ngừng hoạt động',
        };
        return <Badge variant={variants[status] || 'secondary'}>{labels[status] || status}</Badge>;
    };

    const getTypeLabel = (type: string) => {
        return ORGANIZATION_TYPES[type as keyof typeof ORGANIZATION_TYPES] || type;
    };

    return (
        <div className="space-y-6">
            {/* Header trang */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Danh sách tổ chức
                    </h1>
                    <p className="text-sm text-muted-foreground">
                        Quản lý các hợp tác xã, doanh nghiệp và tổ chức trong hệ thống.
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <HelpButton screenKey="organization-list"/>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={fetchOrganizations}
                        disabled={loading}
                    >
                        <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`}/>
                        Làm mới
                    </Button>
                    {canCreate && (
                        <Button variant="create" size="sm" onClick={() => navigate('/organizations/create')}>
                            <PlusCircle className="h-4 w-4 mr-1"/>
                            Tạo tổ chức
                        </Button>
                    )}
                </div>
            </div>

            {/* Thanh tìm kiếm */}
            <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
                <CardContent className="p-4">
                    <div className="relative w-full max-w-md">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
                        <Input
                            placeholder="Tìm theo tên hoặc mã tổ chức..."
                            value={search}
                            onChange={(e) => {
                                setSearch(e.target.value);
                                setPage(0);
                            }}
                            className="pl-9 h-9"
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Bảng danh sách */}
            <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
                <CardContent className="p-4 space-y-4">
                    <div className="rounded-md border overflow-x-auto">
                        <Table>
                            <TableHeader>
                                <TableRow className="bg-muted/50">
                                    <TableHead className="w-12 text-center">STT</TableHead>
                                    <TableHead>Mã tổ chức</TableHead>
                                    <TableHead>Tên tổ chức</TableHead>
                                    <TableHead>Loại</TableHead>
                                    <TableHead>Trạng thái</TableHead>
                                    <TableHead>Ngày tạo</TableHead>
                                    <TableHead className="text-right">Thao tác</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {loading ? (
                                    <TableRow>
                                        <TableCell colSpan={7} className="h-32 text-center text-muted-foreground">
                                            <div className="flex flex-col items-center justify-center gap-2">
                                                <RefreshCw className="w-6 h-6 animate-spin text-emerald-600"/>
                                                <span>Đang tải danh sách tổ chức...</span>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ) : paginated.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={7} className="h-32 text-center text-muted-foreground">
                                            Không tìm thấy tổ chức nào.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    paginated.map((org, index) => (
                                        <TableRow key={org.id} className="hover:bg-muted/40 transition-colors">
                                            <TableCell className="text-center font-medium text-muted-foreground">
                                                {page * PAGE_SIZE + index + 1}
                                            </TableCell>
                                            <TableCell className="font-medium text-slate-900">{org.code}</TableCell>
                                            <TableCell className="font-medium text-slate-900">{org.name}</TableCell>
                                            <TableCell>{getTypeLabel(org.type)}</TableCell>
                                            <TableCell>{getStatusBadge(org.status)}</TableCell>
                                            <TableCell>{new Date(org.createdAt).toLocaleDateString('vi-VN')}</TableCell>
                                            <TableCell className="text-right">
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => navigate(`/organizations/${org.id}`)}
                                                    className="h-8 text-xs"
                                                >
                                                    Xem
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </div>

                    {/* Controls phân trang */}
                    <Pagination
                        currentPage={page}
                        totalPages={totalPages}
                        totalElements={filtered.length}
                        pageSize={PAGE_SIZE}
                        loading={loading}
                        itemLabel="tổ chức"
                        onPageChange={setPage}
                    />
                </CardContent>
            </Card>
        </div>
    );
}