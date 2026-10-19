package com.oinkvalley.schedule_svc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * NATS JetStream 연결·consumer 설정. 값 SOT = infra (k3s/values/schedule) — application.properties 는 env 이름만 잇고 기본값이 없다.
 *
 * @param url           NATS_URL
 * @param durable       NATS_DURABLE — durable base. subject 마다 base-suffix.
 * @param reconnectWait NATS_RECONNECT_WAIT (예: 2s)
 * @param ackWait       NATS_ACK_WAIT (예: 30s)
 * @param maxDeliver    NATS_MAX_DELIVER
 */
@ConfigurationProperties(prefix = "nats")
public record NatsProperties(String url, String durable, Duration reconnectWait, Duration ackWait, int maxDeliver) {
}
