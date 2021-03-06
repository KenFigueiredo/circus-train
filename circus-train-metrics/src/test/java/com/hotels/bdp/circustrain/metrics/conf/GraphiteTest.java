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
package com.hotels.bdp.circustrain.metrics.conf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class GraphiteTest {

  private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
  private final Graphite graphite = new Graphite();

  @Before
  public void before() {
    validator.setProviderClass(HibernateValidator.class);
    validator.afterPropertiesSet();
  }

  @Test
  public void validHostAndPort() {
    graphite.setHost("foo.com:1234");

    Set<ConstraintViolation<Graphite>> violations = validator.validate(graphite);

    assertThat(violations.size(), is(0));
  }

  @Test
  public void missingPort() {
    graphite.setHost("foo");

    Set<ConstraintViolation<Graphite>> violations = validator.validate(graphite);

    assertThat(violations.size(), is(1));
  }

  @Test
  public void nullHost() {
    graphite.setHost(null);

    Set<ConstraintViolation<Graphite>> violations = validator.validate(graphite);

    assertThat(violations.size(), is(0));
  }

}
