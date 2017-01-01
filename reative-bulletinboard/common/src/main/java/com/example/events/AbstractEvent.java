package com.example.events;

/**
 * Created by boris on 9/12/16.
 */
public interface AbstractEvent<T extends AbstractEvent<T>> {

    String getId();

}
