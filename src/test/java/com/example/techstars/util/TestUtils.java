package com.example.techstars.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.util.ResourceUtils;

import java.nio.file.Files;

@UtilityClass
public class TestUtils {

    @SneakyThrows
    public String readResource(String path) {
        return Files.readString(ResourceUtils.getFile("classpath:" + path).toPath());
    }
}