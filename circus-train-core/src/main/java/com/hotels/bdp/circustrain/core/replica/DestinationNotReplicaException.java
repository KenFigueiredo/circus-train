/**
 * Copyright (C) 2016-2019 Expedia, Inc.
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
package com.hotels.bdp.circustrain.core.replica;

import org.apache.hadoop.hive.metastore.Warehouse;
import org.apache.hadoop.hive.metastore.api.Table;

import com.hotels.bdp.circustrain.api.CircusTrainException;
import com.hotels.bdp.circustrain.api.CircusTrainTableParameter;

public class DestinationNotReplicaException extends CircusTrainException {

  private static final long serialVersionUID = 1L;

  DestinationNotReplicaException(
      Table oldReplicaTable,
      String replicaMetastoreUris,
      CircusTrainTableParameter tableParameter) {
    super("Found an existing table '"
        + Warehouse.getQualifiedName(oldReplicaTable)
        + "' in '"
        + replicaMetastoreUris
        + "', but it does not appear to be a replica! Missing table property '"
        + tableParameter.parameterName()
        + "'");
  }

}
