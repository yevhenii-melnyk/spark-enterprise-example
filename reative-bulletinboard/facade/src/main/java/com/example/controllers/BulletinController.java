package com.example.controllers;

import com.example.commands.AddBulletin;
import com.example.messagebus.MessageBus;
import com.example.viewmodels.bulletin.Bulletin;
import com.example.viewmodels.bulletin.BulletinRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;

@RestController
@RequestMapping("/bulletins")
public class BulletinController {

    @Autowired
    protected BulletinRepository repo;

    @Autowired
    protected MessageBus messageBus;

    @GetMapping
    public Flux<Bulletin> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public Mono<Bulletin> create(@RequestBody Mono<AddBulletin> newBulletin) {
        return messageBus.send(newBulletin)
                .flatMap(e -> repo.findOne(e.getId()))
                .next();
    }

}
