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
package com.hotels.bdp.circustrain.integration;

import static org.apache.hadoop.fs.s3a.Constants.ACCESS_KEY;
import static org.apache.hadoop.fs.s3a.Constants.ENDPOINT;
import static org.apache.hadoop.fs.s3a.Constants.SECRET_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import static com.hotels.bdp.circustrain.api.CircusTrainTableParameter.REPLICATION_EVENT;
import static com.hotels.bdp.circustrain.integration.IntegrationTestHelper.DATABASE;
import static com.hotels.bdp.circustrain.integration.IntegrationTestHelper.PART_00000;
import static com.hotels.bdp.circustrain.integration.IntegrationTestHelper.SOURCE_UNPARTITIONED_TABLE;
import static com.hotels.bdp.circustrain.integration.utils.TestUtils.toUri;
import static com.hotels.bdp.circustrain.s3s3copier.aws.AmazonS3URIs.toAmazonS3URI;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fm.last.commons.test.file.ClassDataFolder;
import fm.last.commons.test.file.DataFolder;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableMap;

import com.hotels.bdp.circustrain.common.test.base.CircusTrainRunner;
import com.hotels.bdp.circustrain.common.test.junit.rules.S3ProxyRule;
import com.hotels.bdp.circustrain.core.conf.Security;
import com.hotels.bdp.circustrain.integration.utils.TestUtils;
import com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser;
import com.hotels.bdp.circustrain.s3s3copier.S3S3CopierOptions;
import com.hotels.bdp.circustrain.s3s3copier.aws.AmazonS3ClientFactory;
import com.hotels.bdp.circustrain.s3s3copier.aws.JceksAmazonS3ClientFactory;
import com.hotels.beeju.ThriftHiveMetaStoreJUnitRule;

public class CircusTrainHdfsS3IntegrationTest {
  private static final Logger LOG = LoggerFactory.getLogger(CircusTrainHdfsS3IntegrationTest.class);

  private static final String S3_ACCESS_KEY = "access";
  private static final String S3_SECRET_KEY = "secret";

  private static final String TARGET_UNPARTITIONED_TABLE = "ct_table_u_copy";

  public @Rule ExpectedSystemExit exit = ExpectedSystemExit.none();
  public @Rule TemporaryFolder temporaryFolder = new TemporaryFolder();
  public @Rule DataFolder dataFolder = new ClassDataFolder();
  private int s3ProxyPort = TestUtils.getAvailablePort();
  public @Rule S3ProxyRule s3Proxy = S3ProxyRule
      .builder()
      .withPort(s3ProxyPort)
      .withCredentials(S3_ACCESS_KEY, S3_SECRET_KEY)
      .ignoreUnknownHeaders()
      .build();
  private final Map<String, String> metastoreProperties = ImmutableMap
      .<String, String> builder()
      .put(ENDPOINT, String.format("http://127.0.0.1:%d", s3ProxyPort))
      .put(ACCESS_KEY, S3_ACCESS_KEY)
      .put(SECRET_KEY, S3_SECRET_KEY)
      .build();
  public @Rule ThriftHiveMetaStoreJUnitRule sourceCatalog = new ThriftHiveMetaStoreJUnitRule(DATABASE,
      metastoreProperties);
  public @Rule ThriftHiveMetaStoreJUnitRule replicaCatalog = new ThriftHiveMetaStoreJUnitRule(DATABASE,
      metastoreProperties);

  private File sourceWarehouseUri;
  private File replicaWarehouseUri;
  private File housekeepingDbLocation;

  private IntegrationTestHelper helper;

  private String jceksLocation;
  private AmazonS3ClientFactory s3ClientFactory;

  @Before
  public void init() throws Exception {
    sourceWarehouseUri = temporaryFolder.newFolder("source-warehouse");
    replicaWarehouseUri = temporaryFolder.newFolder("replica-warehouse");
    temporaryFolder.newFolder("db");
    housekeepingDbLocation = new File(new File(temporaryFolder.getRoot(), "db"), "housekeeping");

    helper = new IntegrationTestHelper(sourceCatalog.client());

    jceksLocation = String.format("jceks://file/%s/aws.jceks", dataFolder.getFolder().getAbsolutePath());
    Security security = new Security();
    security.setCredentialProvider(jceksLocation);
    s3ClientFactory = new JceksAmazonS3ClientFactory(security);
  }

  private AmazonS3 newS3Client(String tableUri) {
    AmazonS3URI base = toAmazonS3URI(URI.create(tableUri));
    S3S3CopierOptions s3s3CopierOptions = new S3S3CopierOptions(ImmutableMap
        .<String, Object> builder()
        .put(S3S3CopierOptions.Keys.S3_ENDPOINT_URI.keyName(), s3Proxy.getProxyUrl())
        .build());
    return s3ClientFactory.newInstance(base, s3s3CopierOptions);
  }

  @Test
  public void unpartitionedTable() throws Exception {
    final URI sourceTableUri = toUri(sourceWarehouseUri, DATABASE, SOURCE_UNPARTITIONED_TABLE);
    helper.createUnpartitionedTable(sourceTableUri);
    LOG.info(">>>> Table {} ", sourceCatalog.client().getTable(DATABASE, SOURCE_UNPARTITIONED_TABLE));

    final AmazonS3 client = newS3Client("s3a://replica/");
    client.createBucket("replica");

    exit.expectSystemExitWithStatus(0);
    File config = dataFolder.getFile("unpartitioned-single-table-hdfs-s3-replication.yml");
    CircusTrainRunner runner = CircusTrainRunner
        .builder(DATABASE, sourceWarehouseUri, replicaWarehouseUri, housekeepingDbLocation)
        .sourceMetaStore(sourceCatalog.getThriftConnectionUri(), sourceCatalog.connectionURL(),
            sourceCatalog.driverClassName())
        .replicaMetaStore(replicaCatalog.getThriftConnectionUri())
        .copierOption(S3MapReduceCpOptionsParser.S3_ENDPOINT_URI, s3Proxy.getProxyUrl())
        .replicaConfigurationProperty(ENDPOINT, s3Proxy.getProxyUrl())
        .replicaConfigurationProperty(ACCESS_KEY, s3Proxy.getAccessKey())
        .replicaConfigurationProperty(SECRET_KEY, s3Proxy.getSecretKey())
        .build();
    exit.checkAssertionAfterwards(new Assertion() {
      @Override
      public void checkAssertion() throws Exception {
        // Assert location
        Table hiveTable = replicaCatalog.client().getTable(DATABASE, TARGET_UNPARTITIONED_TABLE);
        String eventId = hiveTable.getParameters().get(REPLICATION_EVENT.parameterName());
        URI replicaLocation = toUri("s3a://replica/", DATABASE, TARGET_UNPARTITIONED_TABLE + "/" + eventId);
        assertThat(hiveTable.getSd().getLocation(), is(replicaLocation.toString()));
        // Assert copied files
        File dataFile = new File(sourceTableUri.getPath(), PART_00000);
        String fileKeyRegex = String.format("%s/%s/ctt-\\d{8}t\\d{6}.\\d{3}z-\\w{8}/%s", DATABASE,
            TARGET_UNPARTITIONED_TABLE, PART_00000);
        List<S3ObjectSummary> replicaFiles = TestUtils.listObjects(client, "replica");
        assertThat(replicaFiles.size(), is(1));
        for (S3ObjectSummary objectSummary : replicaFiles) {
          assertThat(objectSummary.getSize(), is(dataFile.length()));
          assertThat(objectSummary.getKey().matches(fileKeyRegex), is(true));
        }
      }
    });
    runner.run(config.getAbsolutePath());
  }

}
