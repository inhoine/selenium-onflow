# Automation Test Framework (OMS & WMS)

Enterprise-grade Test Automation framework for Order Management System (OMS) and Warehouse Management System (WMS) workflows, built with **Java 25**, **Selenium WebDriver 4**, **TestNG 7**, **Maven**, and **Allure Report**.

---

## 🏗️ Architecture & Design Patterns

The framework follows a multi-tier hybrid automation architecture:

```text
.
├── .github/
│   └── workflows/
│       └── regression-test.yml        # CI/CD Automated Regression Pipeline
├── .vscode/                           # IDE configurations & recommended extensions
├── pom.xml                            # Maven dependencies & plugins (Selenium, TestNG, Allure)
├── testng.xml                         # TestNG suite runner with parallel execution config
├── src/test/resources/
│   ├── config.properties              # Default configuration
│   ├── config.staging.properties      # Staging environment configuration
│   ├── config.prod.properties         # Production environment configuration
│   ├── data/                          # Test payload datasets (JSON)
│   └── logging.properties             # Logging configuration
└── src/test/java/com/example/seleniumtestng/
    ├── base/                          # BaseTest & DriverManager (ThreadLocal<WebDriver>)
    ├── clients/                       # WmsApiClient (REST API integration for test pre-setup)
    ├── config/                        # ConfigReader (Multi-env, .env, JVM system properties)
    ├── flows/                         # Business Flow Orchestration (Inbound, Picking, Auth...)
    ├── listeners/                     # TestListener (Allure attach, screenshots) & RetryAnalyzer
    ├── models/                        # Strongly typed POJOs / Records for API & UI payloads
    ├── pages/                         # Page Object Model (POM) UI locators & component actions
    ├── tests/                         # TestNG test suites annotated with Allure metadata
    ├── tools/                         # Maintenance bots & UI inspector tools
    └── utils/                         # Logging (SLF4J), ScanTable, TestDataReader, Barcode tools
```

### Key Design Highlights
- **Page Object Model (POM)**: Isolates web element locators and low-level interactions inside `pages/`.
- **Business Flow Layer (`flows/`)**: Orchestrates complex multi-step user journeys (e.g. `InboundInspectionFlow`, `PickingPreparationFlow`) across multiple pages and API clients, keeping test classes lean and readable.
- **API-UI Hybrid Acceleration**: Uses `WmsApiClient` to rapidly set up preconditions (create PO, receive inbound, allocate picking baskets, commit wave) via REST APIs, allowing UI tests to focus purely on business-critical UI interactions.
- **Thread-Safe Parallel Execution**: `DriverManager` encapsulates `ThreadLocal<WebDriver>`, enabling concurrent test execution across classes, methods, or test suites without thread collision.
- **Flaky Test Auto-Retry**: Integrated `RetryAnalyzer` and `AnnotationTransformer` automatically retry transient network failures on staging environments.
- **Allure Living Documentation**: Comprehensive Allure annotations (`@Epic`, `@Feature`, `@Story`, `@Severity`) across all test classes, with automatic failure screenshot capture and step logging.

---

## 📋 Prerequisites & Requirements

- **Java Development Kit (JDK)**: Java 25 LTS (recommended) or Java 17+ (configured via `pom.xml` compiler release).
- **Build Tool**: Apache Maven 3.9+
- **Browsers**: Google Chrome, Mozilla Firefox, or Microsoft Edge.
- **Driver Management**: Automated via **Selenium Manager** (built into Selenium 4.33+; no manual driver download required).

---

## ⚙️ Configuration & Environment

The framework dynamically resolves configuration in this priority order:
1. **JVM System Properties** (`-Dkey=value`)
2. **Environment Variables** / `.env` file
3. **Environment-specific properties** (`config.<env>.properties` when running `-Denv=staging`)
4. **Default properties** (`src/test/resources/config.properties`)

### 1. Credentials Setup (`.env`)
Create a `.env` file in the project root (see `.env.example`):

```env
# User credentials
OMS_EMAIL=your-oms-email@example.com
OMS_PASSWORD=your-oms-password
OPS_EMAIL=your-ops-email@example.com
OPS_PASSWORD=your-ops-password
WMS_EMAIL=your-wms-email@example.com
WMS_PASSWORD=your-wms-password

# Target system URLs
WMS_BASE_URL=https://wms.staging.example.com
OMS_BASE_URL=https://oms.staging.example.com
OPS_BASE_URL=https://ops.staging.example.com
```

### 2. Common Flow Toggles
You can toggle features directly via `.env` or `-D` flags:

```env
# Execution controls
BROWSER=chrome              # chrome | firefox | edge
HEADLESS=false              # true: run without UI window (CI/CD)
KEEP_BROWSER_OPEN=false     # true: keep browser open on completion (debugging)
RETRY_COUNT=1               # number of automatic retries on test failure

# Business flow toggles
INBOUND_CREATE_NEW=true     # true: create PO on OMS before WMS inspect; false: use existing INBOUND_PO_CODE
DEFAULT_HANDOVER_ENABLED=true # true: execute handover API after packing
```

---

## 🚀 Test Execution Commands

### 1. Build and Compile
```bash
mvn clean test-compile
```

### 2. Run All Tests via TestNG Suite
```bash
mvn test
# Or explicitly pointing to suite:
mvn -Dsurefire.suiteXmlFiles=testng.xml test
```

### 3. Run on Different Environments
```bash
# Target staging environment
mvn -Denv=staging test

# Target production / UAT environment
mvn -Denv=prod test
```

### 4. Run on Specific Browsers & Headless Mode
```bash
# Run on Microsoft Edge
mvn -Dbrowser=edge test

# Run on Mozilla Firefox
mvn -Dbrowser=firefox test

# Run Headless (ideal for CI/CD)
mvn -DHEADLESS=true test
```

### 5. Run Single Test Class or Test Method
```bash
# Run Pick & Pack workflow
mvn -Dtest=PickPackTest test

# Run Inbound inspection workflow
mvn -Dtest=InboundProductTest test

# Run a specific test method
mvn -Dtest=InboundProductTest#inboundInspectionSerialNegative test
```

### 6. Parallel Test Execution
To run tests concurrently, update `testng.xml`:
```xml
<suite name="Regression Suite" parallel="classes" thread-count="2">
```
Or override from command line:
```bash
mvn test -Dparallel=classes -DthreadCount=2
```

---

## 📊 Test Suites & Coverage

| Test Suite | Module | Description & Scope |
|:---|:---|:---|
| **[`LoginTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/LoginTest.java)** | WMS | Valid authentication, empty credentials, invalid password, fulfillment center (FC) selection. |
| **[`ProductTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/ProductTest.java)** | OMS | Product creation, dimensions & weight validation, data-driven negative field boundary checks. |
| **[`InboundProductTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/InboundProductTest.java)** | OMS / WMS | End-to-end Inbound flow: PO creation on OMS, warehouse receipt, box scanning, product inspection (FIFO dimensions, FEFO expiry dates, Serial unique generation `SNxxxxxx`, Batch/Lot validation), and putaway update. |
| **[`CreateOrderTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/CreateOrderTest.java)** | OMS / WMS | B2C sales order creation on OMS, inventory reservation, and WMS pickup wave / campaign generation. |
| **[`PickPackTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/PickPackTest.java)** | WMS | Complete Pick & Pack execution: trolley/basket assignment, picking commit, B2C/B2B/MSO packing by system suggestion or basket list, material scan, progress validation (`assertAllOrdersPacked`), and courier handover. |
| **[`PackingCampaignTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/PackingCampaignTest.java)** | WMS | Packing campaign preparation, trolley receiving, and packaging material verification. |
| **[`ConvertGoodsTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/ConvertGoodsTest.java)** | WMS | Goods category and status conversion workflows. |
| **[`ReturnRmaInspectionTest`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tests/ReturnRmaInspectionTest.java)** | WMS | Customer return (RMA) inspection, returned serial verification, and condition grading. |

---

## 📈 Allure Test Reporting

The framework generates rich, interactive HTML reports with execution timelines, logs, and screenshots.

```bash
# Generate report and open local preview server
mvn allure:serve

# Generate standalone report in target/site/allure-maven-plugin
mvn allure:report
```

- **Screenshots on Failure**: Automatically captured by [`TestListener`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/listeners/TestListener.java) and attached directly to the failed test step in Allure, as well as saved to `screenshots/`.
- **Live Logs**: Test execution logs are recorded via SLF4J / Logback and streamed into Allure attachments.

---

## 🔄 CI/CD Automation (GitHub Actions)

Continuous Integration is configured in [`.github/workflows/regression-test.yml`](file:///d:/Auto_OF/selenium-onflow/.github/workflows/regression-test.yml):
- Triggers on `push` and `pull_request` to `main`/`master`, or manual execution (`workflow_dispatch`).
- Runs headless test suite on `ubuntu-latest`.
- Automatically publishes Allure HTML Report to **GitHub Pages**.
- Archives test failure screenshots and logs as build artifacts.

---

## 🛠️ Maintenance & Developer Tools

- **[`WmsCurrentUiInspectorTool`](file:///d:/Auto_OF/selenium-onflow/src/test/java/com/example/seleniumtestng/tools/WmsCurrentUiInspectorTool.java)**: Dedicated crawler and inspector utility to scan WMS UI menus, active routes, and DOM structures without polluting test results.
- **Serial Generation**: Automated 10-character unique serial generator (`SN` + timestamp + index) in `InboundProductWmsPage` preventing UI table overflow.
