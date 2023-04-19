/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.contingency.list.*;
import com.powsybl.contingency.contingency.list.criterion.SingleCountryCriterion;
import com.powsybl.contingency.contingency.list.criterion.TwoCountriesCriterion;
import com.powsybl.contingency.contingency.list.criterion.TwoNominalVoltageCriterion;
import com.powsybl.iidm.network.Country;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.EquipmentType;


import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Getter
@NoArgsConstructor
@Schema(description = "Form contingency list")
public class FormContingencyList extends AbstractContingencyList {

    @Schema(description = "Equipment type")
    private String equipmentType;

    @Schema(description = "Nominal voltage 1")
    private NumericalFilter nominalVoltage1;

    @Schema(description = "Nominal voltage 2")
    private NumericalFilter nominalVoltage2;

    @Schema(description = "Countries 1")
    private Set<String> countries1;

    @Schema(description = "Countries 2")
    private Set<String> countries2;

    public FormContingencyList(UUID uuid,
                               Date date,
                               String equipmentType,
                               NumericalFilter nominalVoltage1,
                               NumericalFilter nominalVoltage2,
                               Set<String> countries1,
                               Set<String> countries2) {
        super(new ContingencyListMetadataImpl(uuid, ContingencyListType.FORM, date));
        this.equipmentType =  equipmentType;
        this.nominalVoltage1 =  nominalVoltage1;
        this.nominalVoltage2 =  nominalVoltage2;
        this.countries1 =  countries1;
        this.countries2 =  countries2;
    }

    public FormContingencyList(String equipmentType,
                               NumericalFilter nominalVoltage1,
                               NumericalFilter nominalVoltage2,
                               Set<String> countries1,
                               Set<String> countries2) {
        this(null, null, equipmentType, nominalVoltage1, nominalVoltage2, countries1, countries2);
    }

    @Override
    public ContingencyList toPowsyblContingencyList() {
        AbstractEquipmentCriterionContingencyList contingencyList;
        switch (EquipmentType.valueOf(this.getEquipmentType())) {
            case GENERATOR:
            case STATIC_VAR_COMPENSATOR:
            case SHUNT_COMPENSATOR:
            case BUSBAR_SECTION:
            case DANGLING_LINE:
                contingencyList = new InjectionCriterionContingencyList(
                        this.getId().toString(),
                        this.getEquipmentType(),
                        //TODO: replace with "new SingleCountryCriterion(this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList()))" after new powsybl version
                        this.getCountries1().isEmpty() ? null : new SingleCountryCriterion(this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())),
                        NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage1()),
                        Collections.emptyList(),
                        null
                );
                break;
            case HVDC_LINE:
                contingencyList = new HvdcLineCriterionContingencyList(
                        this.getId().toString(),
                        new TwoCountriesCriterion(
                                this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList()),
                                this.getCountries2().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())
                        ),
                        new TwoNominalVoltageCriterion(
                                NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage1()).getVoltageInterval(),
                                NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage2()).getVoltageInterval()
                        ),
                        Collections.emptyList(),
                        null
                );
                break;
            case LINE:
                contingencyList = new LineCriterionContingencyList(
                        this.getId().toString(),
                        new TwoCountriesCriterion(
                                this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList()),
                                this.getCountries2().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())
                        ),
                        NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage1()),
                        Collections.emptyList(),
                        null
                );
                break;
            case TWO_WINDINGS_TRANSFORMER:
                contingencyList = new TwoWindingsTransformerCriterionContingencyList(
                        this.getId().toString(),
                        //TODO: replace with "new SingleCountryCriterion(this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList()))" after new powsybl version
                        this.getCountries1().isEmpty() ? null : new SingleCountryCriterion(this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())),
                        new TwoNominalVoltageCriterion(
                                NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage1()).getVoltageInterval(),
                                NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage2()).getVoltageInterval()
                        ),
                        Collections.emptyList(),
                        null
                );
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }
        return contingencyList;
    }
}
