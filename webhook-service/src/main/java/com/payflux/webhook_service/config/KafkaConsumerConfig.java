package com.payflux.webhook_service.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

  public static final String PAYMENT_CAPTURED_TOPIC = "payflux.payment.captured";
  public static final String PAYMENT_CAPTURED_DLQ_TOPIC = PAYMENT_CAPTURED_TOPIC + ".DLQ";

  @Bean
  public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
    var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
        (ConsumerRecord<?, ?> record, Exception ex) ->
            new TopicPartition(PAYMENT_CAPTURED_DLQ_TOPIC, record.partition()));

    var backOff = new ExponentialBackOff(1_000L, 2.0);
    backOff.setMaxInterval(120_000L);
    backOff.setMaxElapsedTime(300_000L);

    return new DefaultErrorHandler(recoverer, backOff);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      CommonErrorHandler kafkaErrorHandler) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(kafkaErrorHandler);
    return factory;
  }
}
