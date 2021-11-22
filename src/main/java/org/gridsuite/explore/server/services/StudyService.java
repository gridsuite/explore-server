/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.logging.Level;

import static org.gridsuite.explore.server.services.ServicesConstants.HEADER_USER_ID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class StudyService implements IDirectoryElementsService {
    private static final String ROOT_CATEGORY_REACTOR = "reactor.";

    private static final String STUDY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private final WebClient webClient;
    private String studyServerBaseUri;

    @Autowired
    public StudyService(@Value("${backing-services.study-server.base-uri:http://study-server/}") String studyServerBaseUri,
                            WebClient.Builder webClientBuilder) {
        this.studyServerBaseUri = studyServerBaseUri;
        this.webClient = webClientBuilder.build();
    }

    public void setStudyServerBaseUri(String studyServerBaseUri) {
        this.studyServerBaseUri = studyServerBaseUri;
    }

    public Mono<Void> insertStudyWithExistingCaseFile(UUID studyUuid, String userId, Boolean isPrivate, UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies/cases/{caseUuid}?isPrivate={isPrivate}&studyUuid={studyUuid}")
                .buildAndExpand(caseUuid, isPrivate, studyUuid)
                .toUriString();

        return webClient.post()
                .uri(studyServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> insertStudyWithCaseFile(UUID studyUuid, String userId, Boolean isPrivate, Mono<FilePart> caseFile) {
        return caseFile.flatMap(file -> {
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("caseFile", file);

            String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                            "/studies?isPrivate={isPrivate}&studyUuid={studyUuid}")
                    .buildAndExpand(isPrivate, studyUuid)
                    .toUriString();

            return webClient.post()
                    .uri(studyServerBaseUri + path)
                    .header(HEADER_USER_ID, userId)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .publishOn(Schedulers.boundedElastic())
                    .log(ROOT_CATEGORY_REACTOR, Level.FINE);
        });
    }

    @Override
    public Mono<Void> delete(UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();

        return webClient.delete()
                .uri(studyServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .onStatus(httpStatus -> httpStatus != HttpStatus.OK, ClientResponse::createException)
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }
}
