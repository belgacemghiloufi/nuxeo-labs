/**
 *
 */

package org.nuxeo.labs.operations.document;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author fvadon
 */

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.labs.operation" })
public class GetLastDocumentVersionTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder;

    protected DocumentModel section;

    protected DocumentModel docWithVersions;

    protected DocumentModel docWithNoVersion;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        docWithVersions = session.createDocumentModel("/Folder", "docWithVersions", "File");
        docWithVersions.setPropertyValue("dc:title", "docWithVersions");
        docWithVersions = session.createDocument(docWithVersions);
        session.save();
        docWithVersions = session.getDocument(docWithVersions.getRef());

        docWithNoVersion = session.createDocumentModel("/Folder", "docWithNoVersion", "File");
        docWithNoVersion.setPropertyValue("dc:title", "docWithNoVersion");
        docWithNoVersion = session.createDocument(docWithNoVersion);
        session.save();
        docWithNoVersion = session.getDocument(docWithNoVersion.getRef());

        CreateVersions();
    }

    @After
    public void cleanup() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    protected void CreateVersions() {

        VersioningOption vo = VersioningOption.MAJOR;

        for (int i = 1; i <= 3; i++) {
            docWithVersions.setPropertyValue("dc:description", "" + i);
            docWithVersions.putContextData(VersioningService.VERSIONING_OPTION, vo);
            docWithVersions = DocumentHelper.saveDocument(session, docWithVersions);
        }

    }

    @Test
    public void testGetLastVersion() throws InvalidChainException, OperationException, Exception {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docWithVersions);
        OperationChain chain = new OperationChain("testgetLastVersion");
        chain.add(GetLastDocumentVersion.ID);
        DocumentModel lastVersion = (DocumentModel) service.run(ctx, chain);

        assertNotNull(lastVersion);
        assertEquals("3", (String) lastVersion.getPropertyValue("dc:description"));
        assertEquals("3.0", lastVersion.getVersionLabel());

    }

    @Test
    public void tesGetLastVersionWithCreate() throws Exception {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docWithNoVersion);
        OperationChain chain = new OperationChain("testgetLastVersion");
        chain.add(GetLastDocumentVersion.ID);
        DocumentModel lastVersion = (DocumentModel) service.run(ctx, chain);

        assertNull(lastVersion);
        
        // ===============================
        docWithNoVersion.refresh();
        ctx = new OperationContext(session);
        ctx.setInput(docWithNoVersion);
        chain = new OperationChain("testgetLastVersion");
        chain.add(GetLastDocumentVersion.ID).set("createIfNeeded", true);
        lastVersion = (DocumentModel) service.run(ctx, chain);

        assertNotNull(lastVersion);
        assertEquals("0.1", lastVersion.getVersionLabel());
        
        // ===============================
        ctx = new OperationContext(session);
        ctx.setInput(docWithNoVersion);
        chain = new OperationChain("testgetLastVersion");
        chain.add(GetLastDocumentVersion.ID).set("createIfNeeded", true).set("increment", "Major");
        lastVersion = (DocumentModel) service.run(ctx, chain);

        assertNotNull(lastVersion);
        // Previous test created a version, so we have one and the "increment Major" must have been ignored
        assertEquals("0.1", lastVersion.getVersionLabel());

    }

}
