import React from 'react';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { HelpCircle } from 'lucide-react';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { getActionLabel } from '@/utils/permissionLables';
import type { PermissionItem } from '@/types/permission';

interface PermissionGroupProps {
  resourceLabel: string;
  permissions: PermissionItem[];
  onToggle: (permissionId: number, enabled: boolean) => void;
  disabled?: boolean;
}

export const PermissionGroup: React.FC<PermissionGroupProps> = ({
  resourceLabel,
  permissions,
  onToggle,
  disabled = false,
}) => {
  return (
    <div className="border rounded-lg p-4 space-y-3">
      <h3 className="font-semibold text-lg">{resourceLabel}</h3>
      <div className="space-y-2">
        {permissions.map((perm) => (
          <div key={perm.permissionId} className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-sm">{getActionLabel(perm.action)}</span>
              {perm.description && (
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <HelpCircle className="h-4 w-4 text-muted-foreground cursor-help" />
                    </TooltipTrigger>
                    <TooltipContent>
                      <p className="max-w-xs">{perm.description}</p>
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              )}
              {perm.isDefault && (
                <Badge variant="outline" className="text-xs">Mặc định</Badge>
              )}
            </div>
            <Switch
              checked={perm.isEnabled}
              onCheckedChange={(checked) => onToggle(perm.permissionId, checked)}
              disabled={disabled}
            />
          </div>
        ))}
      </div>
    </div>
  );
};