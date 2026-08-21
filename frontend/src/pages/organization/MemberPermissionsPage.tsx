import { MemberList } from "@/components/organization/MemberList";
import { HelpButton } from "@/components/help/HelpButton";

const MemberPermissionsPage = () => {
  return (
    <>
      
      <div className="flex justify-end">
        <HelpButton screenKey="member-permissions" />
      </div>
      <MemberList />
    </>
  );
};

export default MemberPermissionsPage;