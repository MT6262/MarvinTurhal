// config.ts
const BACKEND = {
    LOCALHOST: "http://localhost:8080/api/v1",
    IPV6: "http://[2001:7c0:2320:1:f816:3eff:fed3:ec51]:8080/api/v1",
    IPV4: "http://192.168.178.53:8080/api/v1"
};

// Wählen Sie das gleiche Backend wie in vite.config.ts
const API_CONFIG = {
    API_BASE_URL: BACKEND.LOCALHOST};

export default API_CONFIG;
