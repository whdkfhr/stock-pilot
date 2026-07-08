package com.arok2.stockpilot.price;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.price.producer.PriceProducer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 브로커 없이 인메모리 EmbeddedKafka로 Producer → 토픽 발행을 검증한다.
 * (CI에서도 실행 가능 — spring-kafka-test의 임베디드 브로커 사용)
 */
@SpringBootTest(properties = "stockpilot.price.collector.enabled=false")
@EmbeddedKafka(topics = "stock-price", partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class PriceKafkaRoundTripTest {

    @Autowired
    private PriceProducer priceProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void 프로듀서가_토픽에_종목코드_키로_시세이벤트를_발행한다() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("roundtrip-test", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "stock-price");

        priceProducer.publish(new StockPriceEvent("005930", 57000, 1000, Instant.parse("2026-01-01T00:00:00Z")));

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, "stock-price", Duration.ofSeconds(10));
        assertThat(record.key()).isEqualTo("005930");
        assertThat(record.value()).contains("005930").contains("57000");
        consumer.close();
    }
}
