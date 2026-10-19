package com.oinkvalley.schedule_svc;

import com.oinkvalley.schedule_svc.config.CalendarProperties;
import com.oinkvalley.schedule_svc.config.NatsProperties;
import com.oinkvalley.profile.v1.ProfileInternalServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.grpc.client.ImportGrpcClients;

/** schedule-svc 진입점. */
@SpringBootApplication
@EnableConfigurationProperties({CalendarProperties.class, NatsProperties.class})
@ImportGrpcClients(target = "profile", types = ProfileInternalServiceGrpc.ProfileInternalServiceBlockingStub.class)
public class ScheduleSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScheduleSvcApplication.class, args);
	}
}
