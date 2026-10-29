package com.apartment.rental;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:sqlserver:.*")
class RentalApartmentApplicationTests {

	@Test
	void contextLoads() {
	}

}
