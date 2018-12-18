package com.kafka.txn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class KafkaService {
    @Autowired
    private MessageRepo repo;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }


    @Transactional("transactionManager")
    public ResponseEntity<Object> mixedTransaction(@RequestParam("count") Integer count) {
        for (int i = 1; i < count; i++) {
            Message message = new Message();
            message.setMessage(new Date() + "");
            message = repo.save(message);

            if (i >= 4) {
                System.out.println("ERROR");
                throw new IllegalArgumentException("i >= 4");

            }
            publish("test-topic", UUID.randomUUID().toString(), message);
        }
        return null;
    }

    public void publish(String topic, String key, Message message) {
        try {
            String eventJson = objectMapper.writeValueAsString(message);
            log.info("Is transactional : " + kafkaTemplate.isTransactional());
            log.info("Publishing on topic {} the event : {}", topic, eventJson);
            kafkaTemplate.send(topic, key, eventJson);
        } catch (JsonProcessingException e) {
            log.error("Error in Json processing for event with key " + key);
        } catch (KafkaException e) {
            log.error("Error in publishing for event with key {}, {}", key, e.getMessage());
        }
    }

}
