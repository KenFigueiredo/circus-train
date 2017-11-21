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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class SourceTableTest {

  private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
  private final SourceTable sourceTable = new SourceTable();

  @Before
  public void before() {
    validator.setProviderClass(HibernateValidator.class);
    validator.afterPropertiesSet();

    sourceTable.setDatabaseName("databaseName");
    sourceTable.setTableName("tableName");
  }

  @Test
  public void typical() {
    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(0));
  }

  @Test
  public void nullDatabaseName() {
    sourceTable.setDatabaseName(null);

    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void emptyDatabaseName() {
    sourceTable.setDatabaseName("");

    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void blankDatabaseName() {
    sourceTable.setDatabaseName(" ");

    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void nullTableName() {
    sourceTable.setTableName(null);

    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void emptyTableName() {
    sourceTable.setTableName("");

    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void partitionLimitTooLow() {
    sourceTable.setPartitionLimit((short) 0);

    Set<ConstraintViolation<SourceTable>> violations = validator.validate(sourceTable);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void qualifiedTableName() {
    assertThat(sourceTable.getQualifiedName(), is("databasename.tablename"));
  }

}
