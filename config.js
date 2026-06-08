const path = require("path");
const dotenv = require("dotenv");

dotenv.config({ path: path.resolve(__dirname, ".env") });

const {
  WMS_BASE_URL = "https://stg-wms.onflow.vn",
  OMS_BASE_URL = "https://stg-oms.onflow.vn",
  OPS_BASE_URL = "https://stg-ops.onflow.vn",
  DEFAULT_FC_NAME = "FC HN",
  TEST_TIMEOUT = "15000",
  DEFAULT_PICKUP_ID = "648195",
  DEFAULT_PACKING_MATERIAL_CODE = "40x20x20",
} = process.env;

const parseIntOrDefault = (value, defaultValue) => {
  const parsed = parseInt(value, 10);
  return Number.isNaN(parsed) ? defaultValue : parsed;
};

module.exports = {
  urls: {
    wms: WMS_BASE_URL,
    oms: OMS_BASE_URL,
    ops: OPS_BASE_URL,
  },
  defaultTimeout: parseIntOrDefault(TEST_TIMEOUT, 15000),
  defaultFcName: DEFAULT_FC_NAME,
  defaultPickupId: DEFAULT_PICKUP_ID,
  defaultPackingMaterialCode: DEFAULT_PACKING_MATERIAL_CODE,
};
