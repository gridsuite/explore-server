package org.gridsuite.explore.server.dto.parameters;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentsContainer {

    @Schema(description = "container id")
    private UUID containerId;

    @Schema(description = "container name")
    private String containerName;

    @JsonCreator(mode = DELEGATING)
    public EquipmentsContainer(String containerId) {
        this.containerId = UUID.fromString(containerId);
    }

    public static List<UUID> getEquipmentsContainerUuids(List<EquipmentsContainer> containers) {
        if (containers == null) {
            return null;
        }
        return containers.stream()
                .map(EquipmentsContainer::getContainerId)
                .toList();
    }
}
