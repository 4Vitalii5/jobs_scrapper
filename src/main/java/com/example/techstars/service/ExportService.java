package com.example.techstars.service;

import java.io.IOException;

public interface ExportService {
    String exportDatabaseToSqlFile() throws IOException;
    String exportJobsByFunctionToSql(String laborFunction) throws IOException;
}