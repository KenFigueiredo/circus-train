/**
 * Copyright (C) 2016-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.bdp.circustrain.core.conf;

import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.hotels.bdp.circustrain.api.Modules;

@Profile({ Modules.REPLICATION })
@Configuration
@ConfigurationProperties(prefix = "")
public class TableReplications {

  private @Valid @NotEmpty List<TableReplication> tableReplications;

  public List<TableReplication> getTableReplications() {
    return tableReplications;
  }

  public void setTableReplications(List<TableReplication> tableReplications) {
    this.tableReplications = tableReplications;
  }

}
