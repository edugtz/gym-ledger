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

export const PUBLIC_CONFIG: PublicConfig = {
  onlineLookupAvailable: true,
  providers: {
    usda: false,
    openFoodFacts: false,
  },
  features: {
    genericFoodSearch: false,
    barcodeLookup: false,
  },
  minQueryLength: 3,
  safeMode: true,
};
