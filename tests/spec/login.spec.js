const { expect } = require("chai");
const loginTestSuite = require("../../tests/login_test");

describe("WMS login scenarios", function () {
  it("should run the login test suite successfully", async function () {
    const results = await loginTestSuite();
    expect(results).to.be.an("array");
    expect(results.some((item) => item.status === "failed")).to.be.false;
  });
});
