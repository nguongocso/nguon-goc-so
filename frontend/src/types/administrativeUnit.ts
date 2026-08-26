export type AdministrativeUnitLevel = 'PROVINCE' | 'COMMUNE';

export interface AdministrativeUnitNode {
  id: string;
  code: string;
  name: string;
  level: AdministrativeUnitLevel;
  children: AdministrativeUnitNode[];
}
