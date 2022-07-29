/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

    public Mono<Void> insertStudyWithExistingCaseFile(UUID studyUuid, String userId, UUID caseUuid, String importParams) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies/cases/{caseUuid}?studyUuid={studyUuid}")
                .buildAndExpand(caseUuid, studyUuid)
                .toUriString();

        return webClient.post()
                .uri(studyServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .body(BodyInserters.fromValue(importParams))
                .retrieve()
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> insertStudyWithCaseFile(UUID studyUuid, String userId, Mono<FilePart> caseFile) {
        return caseFile.flatMap(file -> {
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("caseFile", file);

            String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                            "/studies?studyUuid={studyUuid}")
                    .buildAndExpand(studyUuid)
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

    public Mono<Void> insertStudy(UUID sourceStudyUuid, UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies")
                .queryParam("duplicateFrom", sourceStudyUuid)
                .queryParam("studyUuid", studyUuid)
                .toUriString();

        return webClient.post()
                .uri(studyServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);

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

    @Override
    public Flux<Map<String, Object>> getMetadata(List<UUID> studiesUuids) {
        var ids = studiesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/metadata" + "?ids=" + ids)
            .buildAndExpand()
            .toUriString();
        return webClient.get()
            .uri(studyServerBaseUri + path)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
            })
            .publishOn(Schedulers.boundedElastic())
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<String> getCaseImportParameters(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/cases/{caseUuid}/import-parameters")
                .buildAndExpand(caseUuid)
                .toUriString();

        return webClient.get()
                .uri(studyServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }
}
