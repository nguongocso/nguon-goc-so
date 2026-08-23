import { CreateOrganizationForm } from "@/components/organization/CreateOrganizationForm";
import { CreateOrganizationGuide } from "@/components/organization/CreateOrganizationGuide";

export function CreateOrganizationPage() {
  return (
    <div className="w-full px-4 sm:px-6 lg:px-8 py-6">
      
      {/* Grid 1 cột mặc định, chỉ chuyển 2 cột từ màn hình xl (1280px) */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
        {/* Form: toàn bộ chiều rộng trên mobile/laptop, 8 cột trên desktop lớn */}
        <div className="xl:col-span-8">
          <CreateOrganizationForm />
        </div>
        {/* Guide: ẩn trên mobile/laptop (đã có accordion riêng bên dưới), 4 cột trên desktop lớn */}
        <aside className="hidden xl:block xl:col-span-4">
          <CreateOrganizationGuide />
        </aside>
      </div>
      {/* Trên mobile/laptop, Guide accordion sẽ tự hiển thị bên trong component */}
      <div className="xl:hidden mt-6">
        <CreateOrganizationGuide />
      </div>
    </div>
  );
}