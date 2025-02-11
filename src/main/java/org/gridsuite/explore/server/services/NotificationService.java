/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.explore.server.dto.CaseAlertThresholdMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com
 */
@Service
public class NotificationService {

    public static final String DIRECTORY_UPDATE_BINDING = "publishDirectoryUpdate-out-0";

    public static final String HEADER_USER_MESSAGE = "userMessage";

    public static final String HEADER_UPDATE_TYPE = "updateType";

    public static final String HEADER_UPDATE_TYPE_DIRECTORY = "directories";

    public static final String HEADER_USER_ID = "userId";

    public static final String MESSAGE_LOG = "Sending message : {}";

    private static final String CATEGORY_BROKER_OUTPUT = NotificationService.class.getName() + ".output-broker-messages";

    private static final Logger MESSAGE_OUTPUT_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    private final StreamBridge updatePublisher;

    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationService(StreamBridge updatePublisher,
                               ObjectMapper objectMapper) {
        this.updatePublisher = updatePublisher;
        this.objectMapper = objectMapper;
    }

    private void sendMessage(Message<String> message, String bindingName) {
        MESSAGE_OUTPUT_LOGGER.debug(MESSAGE_LOG, message);
        updatePublisher.send(bindingName, message);
    }

    public void emitUserMessage(String sub, String messageId, CaseAlertThresholdMessage message) {
        try {
            sendMessage(MessageBuilder.withPayload(objectMapper.writeValueAsString(message))
                .setHeader(HEADER_USER_MESSAGE, messageId)
                .setHeader(HEADER_UPDATE_TYPE, HEADER_UPDATE_TYPE_DIRECTORY)
                .setHeader(HEADER_USER_ID, sub)
                .build(), DIRECTORY_UPDATE_BINDING);
        } catch (JsonProcessingException e) {
            MESSAGE_OUTPUT_LOGGER.error("Fail to send message to user !!!");
        }
    }
}
