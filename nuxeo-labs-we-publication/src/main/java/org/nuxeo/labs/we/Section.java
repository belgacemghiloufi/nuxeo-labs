/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Frédéric Vadon
 */

package org.nuxeo.labs.we;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * The root entry for the WebEngine module.
 * 
 * @author fvadon
 */

@Produces("text/html;charset=UTF-8")
@WebObject(type = "Section")
public class Section extends Publication {

    private static final Log log = LogFactory.getLog(Section.class);

    @Override
    @GET
    public Object doGet() {
        return getView("index");
    }

    @Override
    @Path("section/{id}")
    public Resource getsectionContent(@PathParam("id")
    String id) {
        return newObject("Section", id);

    }

    @Override
    protected void initialize(Object... arg) {
        String id = (String) arg[0];
        CoreSession session = null;
        DocumentModel currentSection;
        DocumentModelList childSections = null;
        DocumentModelList childrenContent;
        session = ctx.getCoreSession();
        HashMap<String, DocumentModelList> latestPublicationsMap;
        if (session != null) {

            try {
                currentSection = session.query(
                        "SELECT * FROM Document WHERE ecm:uuid='" + id + "'").get(
                        0);
                childSections = session.query("SELECT * FROM Section WHERE  ecm:currentLifeCycleState != 'deleted' and ecm:parentId='"
                        + currentSection.getId() + "'");
                childrenContent = session.query("SELECT * FROM Document WHERE  ecm:primaryType!= 'Section' and ecm:currentLifeCycleState != 'deleted' and ecm:parentId='"
                        + currentSection.getId() + "'");
                ctx.setProperty("childSections", childSections);
                ctx.setProperty("currentSection", currentSection);
                ctx.setProperty("childrenContent", childrenContent);
                latestPublicationsMap = getLatestPublicationMap(session,
                        childSections);
                ctx.setProperty("latestPublicationsMap", latestPublicationsMap);

                log.warn(childSections.size());
            } catch (NuxeoException e) {
                log.error(e);
            }

        }
    }

}
