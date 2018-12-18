package com.kafka.txn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/kafka")
@Slf4j
public class KafkaController {
    @Autowired
    private KafkaService kafkaService;

    @RequestMapping(value = "/trigger_kafka_test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> testTrigger(@RequestParam("count") Integer count) throws EntityNotFoundException {
        System.out.println("Transaction test");

        return kafkaService.mixedTransaction(count);
    }

}
