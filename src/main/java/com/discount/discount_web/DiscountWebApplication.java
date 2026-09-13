git checkout mainpackage com.discount.discount_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DiscountWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscountWebApplication.class, args);
	}

}
