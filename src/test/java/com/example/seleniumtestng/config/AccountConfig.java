package com.example.seleniumtestng.config;

public final class AccountConfig {
    private AccountConfig() {
    }

    public static Credentials oms() {
        return new Credentials(ConfigReader.required("OMS_EMAIL"), ConfigReader.required("OMS_PASSWORD"));
    }

    public static Credentials ops() {
        return new Credentials(ConfigReader.required("OPS_EMAIL"), ConfigReader.required("OPS_PASSWORD"));
    }

    public static Credentials wms() {
        return new Credentials(ConfigReader.required("WMS_EMAIL"), ConfigReader.required("WMS_PASSWORD"));
    }

    public static final class Credentials {
        private final String email;
        private final String password;

        public Credentials(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String email() {
            return email;
        }

        public String password() {
            return password;
        }
    }
}
