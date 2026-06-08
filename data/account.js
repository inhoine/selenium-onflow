const path = require("path");
const envPath = path.resolve(__dirname, "..", ".env");
require("dotenv").config({ path: envPath });

const {
  OMS_EMAIL,
  OMS_PASSWORD,
  OPS_EMAIL,
  OPS_PASSWORD,
  WMS_EMAIL,
  WMS_PASSWORD,
} = process.env;

const missing = [];
if (!OMS_EMAIL) missing.push("OMS_EMAIL");
if (!OMS_PASSWORD) missing.push("OMS_PASSWORD");
if (!OPS_EMAIL) missing.push("OPS_EMAIL");
if (!OPS_PASSWORD) missing.push("OPS_PASSWORD");
if (!WMS_EMAIL) missing.push("WMS_EMAIL");
if (!WMS_PASSWORD) missing.push("WMS_PASSWORD");

if (missing.length > 0) {
  throw new Error(
    `Missing required environment variables in .env: ${missing.join(", ")}.\n` +
      "Copy .env.example to .env and fill in the values.",
  );
}

module.exports = {
  oms: {
    email: OMS_EMAIL,
    password: OMS_PASSWORD,
  },

  ops: {
    email: OPS_EMAIL,
    password: OPS_PASSWORD,
  },

  wms: {
    email: WMS_EMAIL,
    password: WMS_PASSWORD,
  },
};
