package org.gridsuite.explore.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContingencyDto {

    private String name;
    private String equipmentType;

    private NumericalFilter nominalVoltage1;

    private NumericalFilter nominalVoltage2;

    private Set<String> countries1;


    private Set<String> countries2;


    private ContingencyListMetadataImpl metadata;

}
