export interface PublicConfig {
  onlineLookupAvailable: boolean;
  providers: {
    usda: boolean;
    openFoodFacts: boolean;
  };
  features: {
    genericFoodSearch: boolean;
    barcodeLookup: boolean;
  };
  minQueryLength: number;
  safeMode: boolean;
}

export const MIN_QUERY_LENGTH = 3;
