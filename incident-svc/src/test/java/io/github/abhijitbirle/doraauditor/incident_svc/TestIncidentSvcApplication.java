package io.github.abhijitbirle.doraauditor.incident_svc;

import org.springframework.boot.SpringApplication;

public class TestIncidentSvcApplication {

	public static void main(String[] args) {
		SpringApplication.from(IncidentSvcApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
