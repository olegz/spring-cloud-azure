/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.example;

import com.microsoft.azure.spring.integration.core.AzureHeaders;
import com.microsoft.azure.spring.integration.core.api.CheckpointMode;
import com.microsoft.azure.spring.integration.core.api.Checkpointer;
import com.microsoft.azure.spring.integration.storage.queue.StorageQueueOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

/**
 * @author Miao Cao
 */
@Slf4j
@RestController
public class WebController {

    private static final String STORAGE_QUEUE_NAME = "example";

    @Autowired
    StorageQueueOperation storageQueueOperation;

    @PostMapping("/messages")
    public String send(@RequestParam("message") String message) {
        this.storageQueueOperation.sendAsync(STORAGE_QUEUE_NAME, MessageBuilder.withPayload(message).build());
        return message;
    }

    @GetMapping("/messages")
    public String receive() throws ExecutionException, InterruptedException {
        this.storageQueueOperation.setMessagePayloadType(String.class);
        this.storageQueueOperation.setCheckpointMode(CheckpointMode.MANUAL);
        Message<?> message = this.storageQueueOperation.receiveAsync(STORAGE_QUEUE_NAME).get();
        if(message == null) {
            log.info("You have no new messages.");
            return null;
        }
        log.info("Message arrived! Payload: " + message.getPayload());

        Checkpointer checkpointer = message.getHeaders().get(AzureHeaders.CHECKPOINTER, Checkpointer.class);
        checkpointer.success().handle((r, ex) -> {
            if (ex == null) {
                log.info("Message '{}' successfully checkpointed", message.getPayload());
            }
            return null;
        });

        return (String) message.getPayload();
    }
}
