import apiClient from "./axiosConfig";
import type {
  SeasonYieldComparisonParams,
  SeasonYieldComparisonResponse,
} from "@/types/seasonYieldComparison";

const ENDPOINT = "/reports/crop-area-analysis/season-yield-comparison";

export async function getSeasonYieldComparison(
  params: SeasonYieldComparisonParams,
): Promise<SeasonYieldComparisonResponse> {
  const searchParams = new URLSearchParams();

  params.years.forEach((year) => {
    searchParams.append("years", String(year));
  });

  if (params.farmAreaId) {
    searchParams.set("farmAreaId", params.farmAreaId);
  }
  if (params.productCategoryId) {
    searchParams.set("productCategoryId", params.productCategoryId);
  }
  if (params.organizationId) {
    searchParams.set("organizationId", params.organizationId);
  }

  const response = await apiClient.get<{
    data: SeasonYieldComparisonResponse;
  }>(ENDPOINT, { params: searchParams });

  return response.data.data;
}
