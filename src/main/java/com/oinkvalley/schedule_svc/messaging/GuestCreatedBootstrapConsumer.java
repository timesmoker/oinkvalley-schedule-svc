package com.oinkvalley.schedule_svc.messaging;

import com.oinkvalley.schedule_svc.service.GuestCalendarBootstrapService;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * {@code auth.guest.created} 를 받아 게스트 캘린더 샘플을 만든다.
 * Subject 선택·durable·재시도는 이 서비스 몫. auth 는 사실만 쏜다.
 */
@Component
public class GuestCreatedBootstrapConsumer {

    private static final Logger log = LoggerFactory.getLogger(GuestCreatedBootstrapConsumer.class);

    /** 구독 주소. 발행 쪽과 같은 문자열. */
    static final String SUBJECT = "auth.guest.created";

    private final GuestCalendarBootstrapService bootstrapService;
    private final String natsUrl;
    private final String durableBase;

    private Connection connection;
    private JetStreamSubscription subscription;

    public GuestCreatedBootstrapConsumer(
            GuestCalendarBootstrapService bootstrapService,
            @Value("${nats.url:}") String natsUrl,
            @Value("${nats.durable:schedule-inbox}") String durableBase) {
        this.bootstrapService = bootstrapService;
        this.natsUrl = natsUrl;
        this.durableBase = durableBase == null || durableBase.isBlank() ? "schedule-inbox" : durableBase;
    }

    @PostConstruct
    void start() {
        if (natsUrl == null || natsUrl.isBlank()) {
            log.info("NATS_URL empty — guest bootstrap consumer disabled");
            return;
        }
        String durable = durableBase + "-auth-guest-created";
        try {
            connection = Nats.connect(new Options.Builder()
                    .server(natsUrl)
                    .connectionName("schedule-svc")
                    .maxReconnects(-1)
                    .reconnectWait(Duration.ofSeconds(2))
                    .build());
            JetStream jetStream = connection.jetStream();
            ConsumerConfiguration consumer = ConsumerConfiguration.builder()
                    .durable(durable)
                    .deliverGroup(durable)
                    .ackPolicy(AckPolicy.Explicit)
                    .ackWait(Duration.ofSeconds(30))
                    .maxDeliver(5)
                    .filterSubject(SUBJECT)
                    .build();
            subscription = jetStream.subscribe(
                    SUBJECT,
                    durable,
                    connection.createDispatcher(),
                    this::onMessage,
                    false,
                    PushSubscribeOptions.builder().configuration(consumer).build());
            log.info("nats: subscribed subject={} durable={}", SUBJECT, durable);
        } catch (Exception e) {
            log.warn("nats: guest bootstrap subscribe skipped: {}", e.toString());
            closeQuietly();
        }
    }

    private void onMessage(Message message) {
        long userId;
        try {
            JsonNode body = JsonMapper.shared().readTree(message.getData());
            userId = body.path("userId").asLong(0);
        } catch (RuntimeException e) {
            log.warn("nats: term subject={} bad payload", SUBJECT);
            message.term();
            return;
        }
        if (userId <= 0) {
            log.warn("nats: term subject={} missing userId", SUBJECT);
            message.term();
            return;
        }
        try {
            bootstrapService.bootstrap(userId);
            message.ack();
        } catch (RuntimeException e) {
            log.warn("nats: nak subject={} userId={}: {}", SUBJECT, userId, e.toString());
            message.nak();
        }
    }

    @PreDestroy
    void closeQuietly() {
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (RuntimeException ignored) {
                // 종료 중
            }
            subscription = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            connection = null;
        }
    }
}
