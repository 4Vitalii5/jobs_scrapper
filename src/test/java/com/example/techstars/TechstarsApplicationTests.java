package com.example.techstars;

import com.example.techstars.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TechstarsApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
