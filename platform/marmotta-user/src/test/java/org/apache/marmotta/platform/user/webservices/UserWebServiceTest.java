/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.platform.user.webservices;

import com.jayway.restassured.RestAssured;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.exception.io.MarmottaImportException;
import org.apache.marmotta.platform.core.test.base.JettyMarmotta;
import org.apache.marmotta.platform.user.api.AccountService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.jayway.restassured.RestAssured.expect;

/**
 * UserWebService Test
 *
 * @author Sergio Fernández
 */
public class UserWebServiceTest {

    private static JettyMarmotta marmotta;

    @BeforeClass
    public static void setUp() throws MarmottaImportException, URISyntaxException {
        marmotta = new JettyMarmotta("/marmotta", ConfigurationService.class, UserWebService.class);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = marmotta.getPort();
        RestAssured.basePath = marmotta.getContext();
    }

    @AfterClass
    public static void tearDown() {
        marmotta.shutdown();
    }

    @Test
    @Ignore("internal jboss issue")
    public void testLogin() throws IOException, InterruptedException {
        final ConfigurationService configurationService = marmotta.getService(ConfigurationService.class);
        final String passwd = configurationService.getStringConfiguration("user.admin.password");

        expect().
            log().ifError().
            statusCode(200).
        given().
            auth(). preemptive().basic("admin", passwd).
        when().
            get("/user/login");
    }

}
