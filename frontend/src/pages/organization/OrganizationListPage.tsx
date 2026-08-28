import {useEffect, useState, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {Building2, PlusCircle} from 'lucide-react';
import {TableCell, TableHead, TableRow} from '@/components/ui/table';
import {Button} from '@/components/ui/button';
import {Badge} from '@/components/ui/badge';
import {HelpButton} from '@/components/help/HelpButton';
import {Pagination} from '@/components/common/Pagination';
import {ListPageHeader} from '@/components/common/ListPageHeader';
import {ListCard} from '@/components/common/ListCard';
import {ListToolbar} from '@/components/common/ListToolbar';
import {SearchInput} from '@/components/common/SearchInput';
import {FilterSelect} from '@/components/common/FilterSelect';
import {RefreshButton} from '@/components/common/RefreshButton';
import {DataTableShell} from '@/components/common/DataTableShell';
import {StatusBadge} from '@/components/common/StatusBadge';
import {getOrganizations} from '@/api/organizationApi';
import {type Organization} from '@/types/organization';
import {ORGANIZATION_TYPES} from '@/utils/constants';
import {usePermission} from '@/hooks/usePermission';
import {ROLE_ACCESS} from '@/config/roleAccess';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    {value: 'ALL', label: 'Tất cả trạng thái'},
    {value: 'ACTIVE', label: 'Đang hoạt động'},
    {value: 'INACTIVE', label: 'Ngừng hoạt động'},
];

export function OrganizationListPage() {
    const navigate = useNavigate();
    const [organizations, setOrganizations] = useState<Organization[]>([]);
    const [loading, setLoading] = useState(true);

    const canCreate = usePermission(ROLE_ACCESS.organizationCreate);

    // Tìm kiếm, lọc trạng thái & phân trang (client-side)
    const [search, setSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
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

    // Lọc theo từ khóa + trạng thái (client-side)
    const filtered = useMemo(() => {
        const q = search.toLowerCase().trim();
        return organizations.filter((org) => {
            const matchKeyword =
                !q ||
                org.name.toLowerCase().includes(q) ||
                org.code.toLowerCase().includes(q);
            const matchStatus = statusFilter === 'ALL' || org.status === statusFilter;
            return matchKeyword && matchStatus;
        });
    }, [organizations, search, statusFilter]);

    const totalPages = Math.ceil(filtered.length / PAGE_SIZE);

    // Phân trang client-side
    const paginated = useMemo(() => {
        const start = page * PAGE_SIZE;
        return filtered.slice(start, start + PAGE_SIZE);
    }, [filtered, page]);

    const getTypeLabel = (type: string) => {
        return ORGANIZATION_TYPES[type as keyof typeof ORGANIZATION_TYPES] || type;
    };

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'ACTIVE':
                return <StatusBadge label="Đang hoạt động" tone="success" />;
            case 'INACTIVE':
                return <StatusBadge label="Ngừng hoạt động" tone="danger" />;
            default:
                return <Badge variant="outline">{status}</Badge>;
        }
    };

    const header = (
        <>
            <TableHead className="w-12 text-center">STT</TableHead>
            <TableHead>Mã tổ chức</TableHead>
            <TableHead>Tên tổ chức</TableHead>
            <TableHead>Loại</TableHead>
            <TableHead>Trạng thái</TableHead>
            <TableHead>Ngày tạo</TableHead>
            <TableHead className="text-right">Thao tác</TableHead>
        </>
    );

    const body = paginated.map((org, index) => (
        <TableRow key={org.id} className="hover:bg-muted/40 transition-colors">
            <TableCell className="text-center font-medium text-muted-foreground">
                {page * PAGE_SIZE + index + 1}
            </TableCell>
            <TableCell className="font-medium text-foreground">{org.code}</TableCell>
            <TableCell className="font-medium text-foreground">{org.name}</TableCell>
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
    ));

    return (
        <div className="space-y-6">
            {/* Header trang */}
            <ListPageHeader
                icon={Building2}
                title="Danh sách tổ chức"
                description="Quản lý các hợp tác xã, doanh nghiệp và tổ chức trong hệ thống."
                actions={
                    <>
                        <HelpButton screenKey="organization-list" />
                        {canCreate && (
                            <Button variant="create" size="sm" onClick={() => navigate('/organizations/create')}>
                                <PlusCircle className="h-4 w-4 mr-1" />
                                Tạo tổ chức
                            </Button>
                        )}
                    </>
                }
            />

            {/* Thẻ chung: bộ lọc + bảng + phân trang */}
            <ListCard>
                <ListToolbar
                    left={
                        <>
                            <SearchInput
                                placeholder="Tìm theo tên hoặc mã tổ chức..."
                                value={search}
                                onChange={(e) => {
                                    setSearch(e.target.value);
                                    setPage(0);
                                }}
                            />
                            <FilterSelect
                                value={statusFilter}
                                onValueChange={(val) => {
                                    setStatusFilter(val || 'ALL');
                                    setPage(0);
                                }}
                                options={STATUS_OPTIONS}
                            />
                        </>
                    }
                    right={<RefreshButton onClick={fetchOrganizations} loading={loading} />}
                />

                <DataTableShell
                    header={header}
                    body={body}
                    loading={loading}
                    empty={filtered.length === 0}
                    colSpan={7}
                    loadingMessage="Đang tải danh sách tổ chức..."
                    emptyMessage="Không tìm thấy tổ chức nào."
                />

                <Pagination
                    currentPage={page}
                    totalPages={totalPages}
                    totalElements={filtered.length}
                    pageSize={PAGE_SIZE}
                    loading={loading}
                    itemLabel="tổ chức"
                    onPageChange={setPage}
                />
            </ListCard>
        </div>
    );
}
