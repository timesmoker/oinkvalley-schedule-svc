package com.oinkvalley.schedule_svc.messaging;

import com.oinkvalley.schedule_svc.config.NatsProperties;
import com.oinkvalley.schedule_svc.service.GuestCalendarBootstrapService;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.StreamContext;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.DeliverPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code auth.guest.created} 를 받아 게스트 캘린더 샘플을 만든다.
 * Subject 선택·durable·재시도는 이 서비스 몫. auth 는 사실만 쏜다.
 * <p>
 * durable pull consumer 를 명시적으로 만들어 붙는다 (search·notification 과 같은 방식). 파드가 내려가도 consumer 는
 * 서버에 남고, replica 들은 같은 consumer 에서 나눠 받는다.
 */
@Component
public class GuestCreatedBootstrapConsumer {

    private static final Logger log = LoggerFactory.getLogger(GuestCreatedBootstrapConsumer.class);

    /** 구독 주소. 발행 쪽과 같은 문자열. */
    static final String SUBJECT = "auth.guest.created";

    /** JetStream API 에러 코드 — consumer 없음. */
    private static final int ERR_CONSUMER_NOT_FOUND = 10014;

    private final GuestCalendarBootstrapService bootstrapService;
    private final NatsProperties nats;

    private volatile Connection connection;
    private volatile MessageConsumer consumer;
    private volatile boolean stopped;
    /** subscribeUntilReady 는 한 번에 하나만 */
    private final AtomicBoolean subscribing = new AtomicBoolean();

    public GuestCreatedBootstrapConsumer(GuestCalendarBootstrapService bootstrapService, NatsProperties nats) {
        this.bootstrapService = bootstrapService;
        this.nats = nats;
    }

    /**
     * 기동을 막지 않고 백그라운드에서 연결한다. NATS 가 늦게 떠도(재부팅 순서) 붙을 때까지 재시도하고,
     * 연결되면 consumer 를 설정한다 (실패하면 재연결 간격마다 다시) — 연결 실패로 구독을 끄지 않는다.
     */
    @PostConstruct
    void start() {
        Options options = new Options.Builder()
                .server(nats.url())
                .connectionName("schedule-svc")
                .maxReconnects(-1)
                .reconnectWait(nats.reconnectWait())
                .connectionListener((conn, event) -> {
                    // 첫 연결이 실패한 뒤 붙으면 CONNECTED 가 아니라 RECONNECTED 가 온다 — 둘 다 받는다
                    if (event == ConnectionListener.Events.CONNECTED || event == ConnectionListener.Events.RECONNECTED) {
                        connection = conn;
                        log.info("nats: {} url={}", event, nats.url());
                        if (consumer == null && subscribing.compareAndSet(false, true)) {
                            Thread.ofVirtual().start(this::subscribeUntilReady);
                        }
                    } else if (event == ConnectionListener.Events.DISCONNECTED) {
                        log.warn("nats: disconnected — reconnecting url={}", nats.url());
                    }
                })
                .build();
        try {
            Nats.connectAsynchronously(options, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("nats connect interrupted", e);
        }
    }

    /** consumer 설정이 성공할 때까지 재연결 간격으로 재시도 (스트림이 아직 없을 때 등). */
    private void subscribeUntilReady() {
        try {
            subscribeLoop();
        } finally {
            subscribing.set(false);
        }
    }

    private void subscribeLoop() {
        String durable = nats.durable() + "-auth-guest-created";
        while (consumer == null && !stopped) {
            try {
                JetStreamManagement jsm = connection.jetStreamManagement();
                List<String> streams = jsm.getStreamNames(SUBJECT);
                if (streams.isEmpty()) {
                    throw new IllegalStateException("no stream for " + SUBJECT + " (run nats-ensure-subjects.sh?)");
                }
                String stream = streams.get(0);
                ensureConsumer(jsm, stream, durable);
                StreamContext streamContext = connection.getStreamContext(stream);
                consumer = streamContext.getConsumerContext(durable).consume(this::onMessage);
                log.info("nats: subscribed subject={} stream={} durable={}", SUBJECT, stream, durable);
            } catch (Exception e) {
                log.warn("nats: guest bootstrap subscribe failed, retrying in {}: {}", nats.reconnectWait(), e.toString());
                try {
                    Thread.sleep(nats.reconnectWait());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * durable pull consumer 를 만들거나 설정(ack 대기·재전송 상한)을 맞춘다. 시작 위치는 처음 만들 때만 정해진다.
     * 예전 push consumer 가 남아 있으면 ack 된 위치 다음부터 이어 받도록 pull 로 바꾼다.
     */
    private void ensureConsumer(JetStreamManagement jsm, String stream, String durable)
            throws IOException, JetStreamApiException {
        ConsumerConfiguration.Builder want = ConsumerConfiguration.builder()
                .durable(durable)
                .filterSubject(SUBJECT)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(nats.ackWait())
                .maxDeliver(nats.maxDeliver());

        ConsumerInfo existing;
        try {
            existing = jsm.getConsumerInfo(stream, durable);
        } catch (JetStreamApiException e) {
            if (e.getApiErrorCode() != ERR_CONSUMER_NOT_FOUND) {
                throw e;
            }
            // 처음 만들 때 스트림에 남은 이벤트부터 전부 (게스트 샘플 누락 방지)
            jsm.addOrUpdateConsumer(stream, want.deliverPolicy(DeliverPolicy.All).build());
            return;
        }

        ConsumerConfiguration current = existing.getConsumerConfiguration();
        if (current.getDeliverSubject() != null) {
            long startSeq = existing.getAckFloor().getStreamSequence() + 1;
            log.info("nats: migrating push consumer {} to pull from stream seq {}", durable, startSeq);
            jsm.deleteConsumer(stream, durable);
            jsm.addOrUpdateConsumer(stream, want.deliverPolicy(DeliverPolicy.ByStartSequence).startSequence(startSeq).build());
            return;
        }
        // 이미 pull — 시작 위치는 그대로 두고 바꿀 수 있는 설정만 맞춘다
        jsm.addOrUpdateConsumer(stream, ConsumerConfiguration.builder(current)
                .filterSubject(SUBJECT)
                .ackWait(nats.ackWait())
                .maxDeliver(nats.maxDeliver())
                .build());
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

    /** 받던 메시지를 멈추고 연결을 닫는다. consumer 는 서버에 남는다. */
    @PreDestroy
    void closeQuietly() {
        stopped = true;
        if (consumer != null) {
            consumer.stop();
            consumer = null;
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
