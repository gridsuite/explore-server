package org.gridsuite.explore.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.logging.Level;

@Service
public class StudyService {
    private static final String ROOT_CATEGORY_REACTOR = "reactor.";

    private static final String STUDY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    static final String HEADER_USER_ID = "userId";

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

    public Mono<Void> deleteFromStudyServer(UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();

        return webClient.delete()
                .uri(studyServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .onStatus(httpStatus -> httpStatus != HttpStatus.OK, r -> Mono.empty())
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }
}
