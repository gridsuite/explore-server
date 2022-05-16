package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
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

@Service
public class CaseService implements IDirectoryElementsService {
    private static final String ROOT_CATEGORY_REACTOR = "reactor.";

    private static final String CASE_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private final WebClient webClient;

    private String caseServerBaseUri;

    public void setBaseUri(String actionsServerBaseUri) {
        this.caseServerBaseUri = actionsServerBaseUri;
    }

    @Autowired
    public CaseService(@Value("${backing-services.case-server.base-uri:http://case-server/}") String studyServerBaseUri,
                        WebClient.Builder webClientBuilder) {
        this.caseServerBaseUri = studyServerBaseUri;
        this.webClient = webClientBuilder.build();
    }

    private static Mono<? extends Throwable> wrapRemoteError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .switchIfEmpty(Mono.error(new ExploreException(ExploreException.Type.REMOTE_ERROR, "{\"message\": " + response.statusCode() + "\"}")))
            .flatMap(e -> Mono.error(new ExploreException(ExploreException.Type.REMOTE_ERROR, e)));
    }

    Mono<UUID> importCase(Mono<FilePart> multipartFile) {
        return multipartFile
            .flatMap(file -> {
                MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
                multipartBodyBuilder.part("file", file);

                return webClient.post()
                    .uri(caseServerBaseUri + "/" + CASE_SERVER_API_VERSION + "/cases/private")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .retrieve()
                    .onStatus(httpStatus -> httpStatus != HttpStatus.OK, CaseService::wrapRemoteError)
                    .bodyToMono(UUID.class)
                    .publishOn(Schedulers.boundedElastic())
                    .log(ROOT_CATEGORY_REACTOR, Level.FINE);
            });
    }

    Mono<UUID> createCase(UUID parentCaseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + "/cases")
                .queryParam("duplicateFrom", parentCaseUuid)
                .toUriString();
        return  webClient.post()
                            .uri(caseServerBaseUri + path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(UUID.class);
    }

    @Override
    public Mono<Void> delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + "/cases/{id}")
            .buildAndExpand(id)
            .toUriString();

        return webClient.delete()
            .uri(caseServerBaseUri + path)
            .header(HEADER_USER_ID, userId)
            .retrieve()
            .onStatus(httpStatus -> httpStatus != HttpStatus.OK, ClientResponse::createException)
            .bodyToMono(Void.class)
            .publishOn(Schedulers.boundedElastic())
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }
}
