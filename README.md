# Selenium Learning TestNG

Automation project for OMS/WMS flows using Java, Selenium WebDriver, Maven, and TestNG.

## Structure

```text
.
├── pom.xml
├── testng.xml
├── src/test/resources/config.properties
└── src/test/java/com/example/seleniumtestng/
    ├── base/
    ├── clients/
    ├── config/
    ├── flows/
    ├── models/
    ├── pages/
    ├── tests/
    └── utils/
```

## Requirements

- Java 11+
- Maven
- Google Chrome

Selenium Manager is used by Selenium 4 to resolve browser drivers automatically.

## Configuration

Default URLs and common test values are in:

```text
src/test/resources/config.properties
```

Values in `config.properties` can be overridden with environment variables,
`.env`, or JVM system properties using the same key.

Credentials are read from `.env`, environment variables, or JVM system properties:

```env
OMS_EMAIL=your-oms-email@example.com
OMS_PASSWORD=your-oms-password
OPS_EMAIL=your-ops-email@example.com
OPS_PASSWORD=your-ops-password
WMS_EMAIL=your-wms-email@example.com
WMS_PASSWORD=your-wms-password
```

## Commands

Compile test sources:

```bash
mvn -q -DskipTests test
```

Run all TestNG tests:

```bash
mvn test
```

Run the TestNG suite file:

```bash
mvn -Dsurefire.suiteXmlFiles=testng.xml test
```

Run a single test class:

```bash
mvn -Dtest=LoginTest test
mvn -Dtest=ProductTest test
mvn -Dtest=InboundProductTest test
mvn -Dtest=CreateOrderTest test
mvn -Dtest=PickPackTest test
```

## Test Coverage

- `LoginTest`: WMS login positive and negative scenarios.
- `ProductTest`: OMS product creation and validation scenarios.
- `InboundProductTest`: OMS inbound creation, WMS inspection, putaway API update.
- `CreateOrderTest`: OMS B2C order creation and WMS pickup order creation.
- `PickPackTest`: WMS equipment, picking API flow, and packing UI flow.

Failure screenshots are saved to `screenshots/`.
