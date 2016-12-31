package com.example.viewmodels.bulletin;

import lombok.Value;

import org.springframework.data.annotation.Id;

@Value
public class Bulletin {

    @Id
    String id;

    String date;

    String author;

    String message;
}
