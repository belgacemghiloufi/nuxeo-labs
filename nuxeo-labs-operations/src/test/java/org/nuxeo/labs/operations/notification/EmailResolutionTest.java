/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.labs.operations.notification;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
@Ignore
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.api", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.platform.usermanager.api",
        "org.nuxeo.ecm.platform.usermanager",
        "org.nuxeo.labs.operations.test:OSGI-INF/doc-type-contrib.xml",
        "org.nuxeo.labs.operations.test:OSGI-INF/directory-config.xml",
        "org.nuxeo.labs.operations.test:OSGI-INF/userservice-config.xml" })
public class EmailResolutionTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    private AdvancedSendEmail operation;

    @Before
    public void setup() {
        operation = new AdvancedSendEmail();
        operation.umgr = userManager;
        operation.ctx = new OperationContext(session);
    }

    @Test
    public void shouldResolveEmailAsEmail() throws NuxeoException {
        operation.isStrict = true;
        List<String> result = operation.getEmailFromString("unknown@example.com");

        assertEquals(1, result.size());
        assertEquals("unknown@example.com", result.get(0));
    }

    @Test
    public void shouldResolveUserAsUserEmail() throws NuxeoException {
        operation.isStrict = true;
        List<String> result = operation.getEmailFromString("user:user1");

        assertEquals(1, result.size());
        assertEquals("user1@example.com", result.get(0));

        result = operation.getEmailFromString("user:user3@test.com");

        assertEquals(1, result.size());
        assertEquals("user3@example.com", result.get(0));
    }

    @Test
    public void shouldResolveGroupAsUserEmails() throws NuxeoException {
        operation.isStrict = true;
        List<String> result = operation.getEmailFromString("group:members");

        assertEquals(2, result.size());
        assertEquals("user1@example.com", result.get(0));
        assertEquals("user3@example.com", result.get(1));
    }

    @Test
    public void shouldThrowExceptionIfStrictAndUnkownUserAndGroupAndNotEmail()
            throws NuxeoException {
        operation.isStrict = true;
        try {
            operation.getEmailFromString("toto");
            fail();
        } catch (NuxeoException e) {
            assertEquals("User or group not found and not an email toto",
                    e.getMessage());
        }
    }

    @Test
    public void shouldResolveUserWithoutPrefixAsUserEmailIfNotStrict()
            throws NuxeoException {
        operation.isStrict = true;
        try {
            operation.getEmailFromString("user1");
            fail();
        } catch (NuxeoException e) {
            assertEquals("User or group not found and not an email user1",
                    e.getMessage());
        }

        operation.isStrict = false;
        List<String> result = operation.getEmailFromString("user1");

        assertEquals(1, result.size());
        assertEquals("user1@example.com", result.get(0));

        result = operation.getEmailFromString("user3@test.com");

        assertEquals(1, result.size());
        assertEquals("user3@example.com", result.get(0));

    }

    @Test
    public void shouldResolveListOfMixedValueAccordingStringResolution()
            throws NuxeoException {
        operation.isStrict = true;
        List<String> value = Arrays.asList(new String[] {
                "unknown@example.com", "user:user2", "user:user3@test.com",
                "group:members" });
        List<String> result = operation.getEmails(value, "TO");

        assertEquals(5, result.size());
        assertEquals("unknown@example.com", result.get(0));
        assertEquals("user2@example.com", result.get(1));
        assertEquals("user3@example.com", result.get(2));
        assertEquals("user1@example.com", result.get(3));
        assertEquals("user3@example.com", result.get(4));

    }

}
