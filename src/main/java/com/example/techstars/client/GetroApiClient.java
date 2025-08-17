package com.example.techstars.client;

import com.example.techstars.dto.GetroApiRequest;
import com.example.techstars.dto.GetroJobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "getroApiClient", url = "https://api.getro.com")
public interface GetroApiClient {

    @PostMapping(value = "/api/v2/collections/89/search/jobs", headers = {
            "Content-Type=application/json",
            "Accept=application/json",
            "User-Agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    })
    GetroJobResponse searchJobsByFunction(@RequestBody GetroApiRequest request);
}