package com.example.messagebus;

import java.time.Duration;
import javax.annotation.PostConstruct;

import com.example.commands.AbstractCommand;
import com.example.events.AbstractEvent;
import com.example.events.CmdCompleted;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.Receiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.Sender;
import reactor.kafka.sender.SenderRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Collections.singletonList;

@Component
public class MessageBus {

    @Autowired
    protected Sender<String, String> producer;

    @Autowired
    private ReceiverOptions<String, String> receiverOptions;

    private ReceiverOptions<String, String> opts;

    @Value("${kafka.commands-topic}")
    protected String commandsTopic;

    @Value("${kafka.events-topic}")
    protected String eventsTopic;

    private ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    private void postConstruct() {
        opts = receiverOptions.subscription(singletonList(eventsTopic)).commitInterval(Duration.ZERO);
    }

    public <T extends AbstractCommand<T>> Mono<CmdCompleted> send(Mono<T> cmd) {
        Mono<SenderRecord<String, String, String>> senderRecord = cmdToRecord(cmd);
        return producer.send(senderRecord, true)
                .next()
                .flatMap(sr -> receiveEvent(sr.correlationMetadata()))
                .next().publishOn(Schedulers.parallel());
    }

    private <T extends AbstractCommand<T>> Mono<SenderRecord<String, String, String>> cmdToRecord(Mono<T> command) {
        return command.map(cmd -> SenderRecord.create(
                new ProducerRecord<>(commandsTopic, toString(cmd)), cmd.getId())
        );
    }

    private Flux<CmdCompleted> receiveEvent(String id) {
        return Receiver.create(opts).receive()
                .map(rr -> fromString(rr.record().value(), CmdCompleted.class))
                .filter(event -> id.equals(event.getId()));
    }

    public <T extends AbstractEvent<T>> Mono<Void> send(final T event) {
        return producer.send(Mono.fromSupplier(() -> new ProducerRecord<>(eventsTopic, toString(event))));
    }

    @SneakyThrows
    private <T> String toString(T obj) {
        return mapper.writeValueAsString(obj);
    }

    @SneakyThrows
    private <T> T fromString(String str, Class<T> clazz) {
        return mapper.readValue(str, clazz);
    }

}
