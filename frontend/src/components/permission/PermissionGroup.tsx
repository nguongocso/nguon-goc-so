import React from 'react';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { HelpCircle, Layers, CheckSquare, Square } from 'lucide-react';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { getActionLabel } from '@/utils/permissionLables';
import type { PermissionItem } from '@/types/permission';

interface PermissionGroupProps {
  resource: string;
  resourceLabel: string;
  permissions: PermissionItem[];
  onToggle: (permissionId: number, enabled: boolean) => void;
  disabled?: boolean;
}

export const PermissionGroup: React.FC<PermissionGroupProps> = ({
  resource,
  resourceLabel,
  permissions,
  onToggle,
  disabled = false,
}) => {
  const isEventChain =
    resource === 'chain_event' ||
    resource === 'event_chain' ||
    resource === 'farm_log' ||
    resourceLabel.toLowerCase().includes('sự kiện') ||
    resourceLabel.toLowerCase().includes('canh tác');

  const allEnabled = permissions.length > 0 && permissions.every((p) => p.isEnabled);

  const handleToggleAll = () => {
    const nextState = !allEnabled;
    permissions.forEach((p) => {
      onToggle(p.permissionId, nextState);
    });
  };

  return (
    <div
      className={`border rounded-xl p-5 space-y-4 transition-all duration-200 ${
        isEventChain
          ? 'border-emerald-200 bg-emerald-50/30 shadow-sm'
          : 'border-slate-200 bg-white hover:border-slate-300'
      }`}
    >
      <div className="flex items-center justify-between pb-2 border-b border-slate-100">
        <div className="flex items-center gap-2">
          {isEventChain && <Layers className="h-5 w-5 text-emerald-600 shrink-0" />}
          <h3 className="font-bold text-base text-slate-800">{resourceLabel}</h3>
          {isEventChain && (
            <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200 text-xs">
              Sự kiện chuỗi
            </Badge>
          )}
        </div>

        {permissions.length > 1 && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleToggleAll}
            disabled={disabled}
            className="h-7 px-2 text-xs text-slate-600 hover:text-emerald-700 hover:bg-emerald-50 gap-1"
          >
            {allEnabled ? (
              <>
                <Square className="h-3.5 w-3.5" /> Bỏ chọn tất cả
              </>
            ) : (
              <>
                <CheckSquare className="h-3.5 w-3.5" /> Chọn tất cả
              </>
            )}
          </Button>
        )}
      </div>

      <div className="space-y-3">
        {permissions.map((perm) => (
          <div
            key={perm.permissionId}
            className="flex items-center justify-between p-2 rounded-lg hover:bg-white/80 transition-colors"
          >
            <div className="flex items-center gap-2 flex-1 mr-3">
              <span className="text-sm font-medium text-slate-700">
                {getActionLabel(perm.action)}
              </span>
              {perm.description && (
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <HelpCircle className="h-4 w-4 text-slate-400 cursor-help shrink-0 hover:text-slate-600" />
                    </TooltipTrigger>
                    <TooltipContent>
                      <p className="max-w-xs text-xs">{perm.description}</p>
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              )}
              {perm.isDefault && (
                <Badge variant="outline" className="text-[10px] py-0 px-1.5 text-slate-500 bg-slate-50">
                  Mặc định
                </Badge>
              )}
            </div>

            <Switch
              checked={perm.isEnabled}
              onCheckedChange={(checked) => onToggle(perm.permissionId, checked)}
              disabled={disabled}
              className="data-[state=checked]:bg-emerald-600"
            />
          </div>
        ))}
      </div>
    </div>
  );
};