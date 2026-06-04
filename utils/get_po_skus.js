// utils/get_po_skus.js

const axios = require("axios");

async function getPOSkus(poCode, token) {
  const response = await axios.get(
    `https://stg-wms.onflow.vn/v1/po/detail/${poCode}/`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
        accept: "application/json",
      },
    },
  );

  const poSkus = response.data?.data?.po_skus || [];

  if (!poSkus.length) {
    throw new Error(`Không tìm thấy SKU trong PO: ${poCode}`);
  }

  return poSkus.map((sku) => ({
    boxCode: sku.box_code,
    quantityInbound: sku.quantity_inbound,
    partnerCode: sku.goods_id?.partner_code,

    // thêm các field dimensions
    goodsW: sku.goods_id?.goods_w,
    goodsD: sku.goods_id?.goods_d,
    goodsH: sku.goods_id?.goods_h,
    goodsWeight: sku.goods_id?.goods_weight,
  }));
}

module.exports = getPOSkus;
