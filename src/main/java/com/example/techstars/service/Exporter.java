package com.example.techstars.service;

import java.io.IOException;

public interface Exporter {

    String exportByFunction(String laborFunction) throws IOException;

    String getFormat();
}