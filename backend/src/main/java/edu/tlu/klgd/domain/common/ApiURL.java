package edu.tlu.klgd.domain.common;

public final class ApiURL {
    private static final String ALL_SUB_PATHS = "/**";

    private ApiURL() {
    }

    public static final String IMPORT_BATCH = "/api/import-batches";
    public static final String DASHBOARD = "/api/dashboard";
    public static final String WORKLOAD = "/api/workloads";
    public static final String REPORT = "/api/reports";
    public static final String AUTH = "/api/auth";
    public static final String ADMIN = "/api/admin";
    public static final String LOGIN = "/login";
    public static final String ME = "/me";
    public static final String USERS = "/users";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String CHANGE_PASSWORD = "/change-password";
    public static final String TEACHER_OPTIONS = "/teacher-options";
    public static final String OVERVIEW = "/overview";
    public static final String TEACHERS = "/teachers";
    public static final String TEACHER_DETAILS = "/teacher-details";
    public static final String DEPARTMENTS = "/departments";
    public static final String RULES = "/rules";
    public static final String RECALCULATE = "/recalculate";
    public static final String WORKLOAD_EXCEL = "/workload.xlsx";
    public static final String ID = "/{id}";
    public static final String ID_RECORDS = "/{id}/records";
    public static final String ID_CALCULATE = "/{id}/calculate";
    public static final String AUTH_LOGIN = AUTH + LOGIN;
    public static final String AUTH_ME = AUTH + ME;
    public static final String AUTH_FORGOT_PASSWORD = AUTH + FORGOT_PASSWORD;
    public static final String AUTH_RESET_PASSWORD = AUTH + RESET_PASSWORD;
    public static final String AUTH_CHANGE_PASSWORD = AUTH + CHANGE_PASSWORD;

    public static String allSubPaths(String apiUrl) {
        return apiUrl + ALL_SUB_PATHS;
    }
}
