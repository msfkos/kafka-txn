package com.kafka.txn;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableTransactionManagement
public class KafkaConfig {

    @Value("${kafka.bootstrap.servers}")
    private String kafkaServers;

    @Value("${kafka.async.core.pool}")
    private Integer kafkaCorePoolSize;

    @Value("${kafka.async.max.pool}")
    private Integer kafkaMaxPoolSize;

    @Bean(name = "kafkaEventPublisher")
    public TaskExecutor kafkaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(kafkaCorePoolSize);
        executor.setMaxPoolSize(kafkaMaxPoolSize);
        executor.setThreadNamePrefix("KAFKA-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        configProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        //configProperties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "transactionalId");
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(configProperties);
        producerFactory.setTransactionIdPrefix("spring-kafka-transaction");
        return producerFactory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    public ConsumerFactory<String, String> consumerFactory(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory("kafka-txn-test"));
        factory.getContainerProperties().setTransactionManager(kafkaTransactionManager());
        return factory;
    }


    @Bean
    public KafkaTransactionManager kafkaTransactionManager() {
        KafkaTransactionManager ktm = new KafkaTransactionManager(producerFactory());
        ktm.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
        ktm.setNestedTransactionAllowed(true);
        return ktm;
    }


    @Bean
    public JpaTransactionManager jpaTransactionManager() {
        return new JpaTransactionManager();
    }

    @Bean(name = "transactionManager")
    public ChainedTransactionManager chainedTransactionManager(JpaTransactionManager jpaTransactionManager,
                                                               KafkaTransactionManager kafkaTransactionManager) {
        return new ChainedTransactionManager(jpaTransactionManager, kafkaTransactionManager);
    }

//    @Bean(name = "transactionManager")
//    public ChainedTransactionManager chainedTxM(JpaTransactionManager jpa, KafkaTransactionManager<?, ?> kafka) {
//        return new ChainedTransactionManager(jpa, kafka);
//    }
}
