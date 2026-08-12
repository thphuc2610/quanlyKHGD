package edu.tlu.klgd.domain.service;

import java.io.IOException;

public interface ReportService {
    byte[] exportWorkloadWorkbook(String academicYear, String semester) throws IOException;
}
