package org.gridsuite.explore.server.dto;

import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gridsuite.explore.server.utils.IdentifierType;

import java.util.List;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Identifier {

    IdentifierType type;
    List<IdentifierItem> identifierList;


    private class IdentifierItem {
        private IdentifierType type;
          private String identifier ;
    }

}
