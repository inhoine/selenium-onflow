async function receivedPOAtWarehouse(inboundCode, token) {
  const url = `https://stg-wms.onflow.vn/v1/po/received-po-at-warehouse/${inboundCode}/?`;

  const body = {
    status_id: 101,
    shipment_images: [
      {
        image_urls:
          "https://nhl.sgp1.cdn.digitaloceanspaces.com/ts/uploads/2026/05/19/cabd764512824dca82e47f9c5045fba6.jpg",
      },
      {
        image_urls:
          "https://nhl.sgp1.cdn.digitaloceanspaces.com/ts/uploads/2026/05/19/662a0f7066a7431fb1e7b704cd1ff363.jpg",
      },
      {
        image_urls:
          "https://nhl.sgp1.cdn.digitaloceanspaces.com/ts/uploads/2026/05/19/6390648a8c654cdb844355f7ebec9d40.jpg",
      },
    ],
    reason_for_refusal: "",
    delivery_drive_name: "thuhn",
    delivery_drive_phone: "2525",
    delivery_drive_license_number: "uimoo",
  };

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(`API failed: ${response.status} - ${JSON.stringify(data)}`);
  }

  console.log("Received PO success:", inboundCode);
  return data;
}

async function updatePutaway(inboundCode, token) {
  const todoUrl =
    "https://stg-wms.onflow.vn/v1/putaway/todo?page_size=100&page=1&stock_level=A";

  const todoResponse = await fetch(todoUrl, {
    method: "GET",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
  });

  const todoData = await todoResponse.json();

  if (!todoResponse.ok) {
    throw new Error(
      `Get putaway todo failed: ${todoResponse.status} - ${JSON.stringify(todoData)}`,
    );
  }

  const targetTasks = (todoData?.data || []).filter(
    (item) => item.shipment_id?.shipment_po === inboundCode,
  );

  if (!targetTasks.length) {
    throw new Error(`Không tìm thấy putaway task cho PO ${inboundCode}`);
  }

  for (const task of targetTasks) {
    const body = {
      dr_id: task.dr_id,
      location_code: "A-01-02-006",
      quantity_putaway: task.quantity,
    };

    const updateResponse = await fetch(
      "https://stg-wms.onflow.vn/v1/putaway/update?",
      {
        method: "POST",
        headers: {
          "Content-Type": "Application/json",
          Accept: "Application/json",
          Authorization: `${token}`,
          "x-client-platform": "mobile",
          "accept-language": "vi",
        },
        body: JSON.stringify(body),
      },
    );

    const updateData = await updateResponse.json();

    if (!updateResponse.ok) {
      throw new Error(
        `Update putaway failed: ${updateResponse.status} - ${JSON.stringify(updateData)}`,
      );
    }

    console.log(`Putaway success: dr_id=${task.dr_id}, qty=${task.quantity}`);
  }
}

async function getPickupDetail(pickupId, token) {
  const url = `https://stg-wms.onflow.vn/v1/pickup/detail/${pickupId}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json, text/plain, */*",
      Authorization: `Bearer ${token}`,
      "accept-language": "vi",
    },
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Get pickup detail failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  const pickupOrders = data?.data?.pickup_orders || [];

  return pickupOrders.flatMap((order) =>
    (order.list_items || []).map((item) => ({
      trackingCode: order.tracking_code,
      partnerCode: item.goods_id?.partner_code,
      goodsCode: item.goods_id?.goods_code,
      barcodes: item.goods_id?.barcodes || [],
      quantitySold: item.quantity_sold,
      quantityPick: item.quantity_pick,
    })),
  );
}

async function getPickupPackingOrders(pickupId, token) {
  const url = `https://stg-wms.onflow.vn/v1/pickup/detail/${pickupId}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json, text/plain, */*",
      Authorization: `Bearer ${token}`,
      "accept-language": "vi",
    },
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Get pickup detail failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  const pickupOrders = data?.data?.pickup_orders || [];

  return pickupOrders.map((order) => ({
    trackingCode: order.tracking_code,
    items: (order.list_items || []).map((item) => ({
      partnerCode: item.goods_id?.partner_code,
      goodsCode: item.goods_id?.goods_code,
      barcodes: item.goods_id?.barcodes || [],
      quantitySold: item.quantity_sold,
      quantityPick: item.quantity_pick,
    })),
  }));
}

async function mapTrolleyPicking(pickupCode, trolleyCode, token) {
  const url = `https://stg-wms.onflow.vn/v1/trolley/trolley-map-picking/${pickupCode}?`;

  const body = {
    trolley_code: trolleyCode,
    skip_trolley_code: false,
  };

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `Bearer ${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Map trolley picking failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  console.log(`Mapped trolley ${trolleyCode} to pickup ${pickupCode}`);
  return data;
}

async function getPickupBinsets(pickupCode, token) {
  const url = `https://stg-wms.onflow.vn/v1/trolley/binset/${pickupCode}?is_issue=0&picking_mode=pattern`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `Bearer ${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Get pickup binset failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  return (data?.data || []).map((item) => ({
    binCode: item.bin_code,
    quantitySold: item.quantity_sold,
    quantityPick: item.quantity_pick,
  }));
}

async function getPickingProductsByBin(pickupCode, binCode, token) {
  const url = `https://stg-wms.onflow.vn/v1/trolley/picking/${pickupCode}?bin_code=${binCode}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `Bearer ${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Get picking products failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  return (data?.data || []).map((item) => ({
    binCode: item.bin_code,
    quantitySold: item.quantity_sold,
    quantityPick: item.quantity_pick,
    stockLevel: item.stock_level,
    batchLotCode: item.batch_lot_code_assign,
    goodsId: item.goods_id?.goods_id,
    goodsName: item.goods_id?.goods_name,
    goodsCode: item.goods_id?.goods_code,
    partnerSku: item.goods_id?.partner_sku,
    partnerCode: item.goods_id?.partner_code,
    barcodes: item.barcodes || [],
    trolleyId: item.trolley_id,
    pickupId: item.pickup_id,
  }));
}

async function updateTrolleyDetail(
  pickupCode,
  binCode,
  barcode,
  quantity,
  token,
) {
  const url = `https://stg-wms.onflow.vn/v1/trolley/detail/${pickupCode}?`;

  const body = {
    bin_code: binCode,
    goods_code: barcode,
    quantity,
  };

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `Bearer ${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Update trolley detail failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  console.log(`Picked barcode=${barcode}, bin=${binCode}, qty=${quantity}`);
  return data;
}

async function pickAllProductsInPickup(pickupCode, token) {
  const binsets = await getPickupBinsets(pickupCode, token);

  if (!binsets.length) {
    throw new Error(`Không tìm thấy vị trí cần pick cho pickup ${pickupCode}`);
  }

  for (const bin of binsets) {
    const products = await getPickingProductsByBin(
      pickupCode,
      bin.binCode,
      token,
    );

    for (const product of products) {
      const barcode = product.barcodes[0];

      if (!barcode) {
        throw new Error(`Không có barcode cho sản phẩm ${product.partnerCode}`);
      }

      const quantityNeedPick = product.quantitySold - product.quantityPick;

      if (quantityNeedPick <= 0) {
        console.log(`Skip ${product.partnerCode}: đã pick đủ`);
        continue;
      }

      await updateTrolleyDetail(
        pickupCode,
        product.binCode,
        barcode,
        quantityNeedPick,
        token,
      );
    }
  }

  console.log(`Pick all products done for pickup ${pickupCode}`);
}

async function commitPickingPickup(pickupCode, trolleyCode, token) {
  const url = `https://stg-wms.onflow.vn/v1/trolley/commit-status/${pickupCode}?`;

  const body = {
    trolley_code: trolleyCode,
    step_count: 0,
  };

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "Application/json",
      Accept: "Application/json",
      Authorization: `Bearer ${token}`,
      "x-client-platform": "mobile",
      "accept-language": "vi",
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      `Commit picking failed: ${response.status} - ${JSON.stringify(data)}`,
    );
  }

  console.log(
    `Commit picking success | pickup=${pickupCode} | trolley=${trolleyCode}`,
  );

  console.log(data);

  return data;
}

module.exports = {
  receivedPOAtWarehouse,
  updatePutaway,
  getPickupDetail,
  getPickupPackingOrders,
  mapTrolleyPicking,
  getPickupBinsets,
  getPickingProductsByBin,
  updateTrolleyDetail,
  pickAllProductsInPickup,
  commitPickingPickup,
};
