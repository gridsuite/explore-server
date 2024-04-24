package org.gridsuite.explore.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.explore.server.services.SupervisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/supervision")
@Tag(name = "explore-server - Supervision")
public class SupervisionController {
    private final SupervisionService supervisionService;

    public SupervisionController(SupervisionService supervisionService) {
        this.supervisionService = supervisionService;
    }

    @DeleteMapping(value = "/explore/elements", params = "ids")
    @Operation(summary = "Remove directories/elements")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "directories/elements was successfully removed")})
    public ResponseEntity<Void> deleteElements(@RequestParam("ids") List<UUID> elementsUuid,
                                               @RequestHeader("userId") String userId) {
        supervisionService.deleteElements(elementsUuid, userId);
        return ResponseEntity.ok().build();
    }
}
