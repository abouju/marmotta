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
package org.apache.marmotta.platform.user.services;

import com.jayway.restassured.RestAssured;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.exception.io.MarmottaImportException;
import org.apache.marmotta.platform.core.test.base.JettyMarmotta;
import org.apache.marmotta.platform.user.api.AccountService;
import org.apache.marmotta.platform.user.model.UserAccount;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;

/**
 * AccountService Test
 *
 * @author Sergio Fernández
 */
public class AccountServiceTest {

    private static Logger log = LoggerFactory.getLogger(AccountServiceTest.class);

    private static JettyMarmotta marmotta;

    @BeforeClass
    public static void setUp() throws MarmottaImportException, URISyntaxException {
        marmotta = new JettyMarmotta("/marmotta", ConfigurationService.class, AccountService.class);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = marmotta.getPort();
        RestAssured.basePath = marmotta.getContext();
    }

    @AfterClass
    public static void tearDown() {
        marmotta.shutdown();
    }

    @Test
    public void testDefaultAccounts() {
        final AccountService accountService = marmotta.getService(AccountService.class);

        final List<UserAccount> accounts = accountService.listAccounts();
        Assert.assertEquals(2, accounts.size());
        Assert.assertEquals("anonymous", accounts.get(0).getLogin());
        Assert.assertEquals("admin", accounts.get(1).getLogin());
    }

    @Test
    public void testAdminDefaults() {
        final AccountService accountService = marmotta.getService(AccountService.class);
        final ConfigurationService configurationService = marmotta.getService(ConfigurationService.class);
        final String passwd = configurationService.getStringConfiguration("user.admin.password");

        final UserAccount admin = accountService.getAccount("admin");
        final Set<String> roles = admin.getRoles();
        Assert.assertEquals(3, roles.size());
        Assert.assertThat(roles, hasItems("user", "editor", "manager"));
        Assert.assertTrue(admin.checkPasswd(passwd));
    }

}
