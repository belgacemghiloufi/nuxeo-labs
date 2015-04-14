/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     thibaud
 */

package org.nuxeo.labs.images;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.PictureViewImpl;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPictureAdapter;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 */
@Operation(id = ImageCropInViewsOp.ID, category = Constants.CAT_CONVERSION, label = "ImageCropInViewsOp", description = "")
public class ImageCropInViewsOp {

    public static final String ID = "ImageCropInViewsOp";

    @Context
    protected CoreSession session;

    @Param(name = "title", required = false)
    protected String title = "";

    @Param(name = "top", required = false)
    protected long top = 0;

    @Param(name = "left", required = false)
    protected long left = 0;

    @Param(name = "width", required = false)
    protected long width = 0;

    @Param(name = "height", required = false)
    protected long height = 0;

    @Param(name = "pictureWidth", required = false)
    protected long pictureWidth = 0;

    @Param(name = "pictureHeight", required = false)
    protected long pictureHeight = 0;

    @Param(name = "targetFileName", required = false)
    protected String targetFileName = "";

    @Param(name = "targetFileNameSuffix", required = false)
    protected String targetFileNameSuffix = "";

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws IOException {

        // Possibly, nothing to do.
        if (width == 0 || height == 0) {
            return inDoc;
        }

        if (!inDoc.hasFacet("Picture")) {
            throw new ClientException(
                    String.format(
                            "The document (id:'%s') with title '%s' doesn't have the 'Picture' facet",
                            inDoc.getId(), inDoc.getTitle()));
        }

        Blob pictureBlob = (Blob) inDoc.getPropertyValue("file:content");

        // Scale the crop
        if (pictureWidth > 0 && pictureHeight > 0) {
            double coef = 0.0;
            int w;
            int h;
            
            ImagingService imagingService = Framework.getService(ImagingService.class);
            ImageInfo info = imagingService.getImageInfo(pictureBlob);
            w = info.getWidth();
            h = info.getHeight();

            if (w != (int) pictureWidth) {
                coef = (double) w / (double) pictureWidth;
                left *= coef;
                width *= coef;
            }
            if (h != (int) pictureHeight) {
                coef = (double) h / (double) pictureHeight;
                top *= coef;
                height *= coef;
            }
        }

        if (targetFileNameSuffix == null || targetFileNameSuffix.isEmpty()) {
            targetFileNameSuffix = "-crop" + top + "-" + left + "-" + width
                    + "x" + height;
        }
        targetFileName = ConversionUtils.updateTargetFileName(pictureBlob,
                targetFileName, targetFileNameSuffix);

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("targetFilePath", targetFileName);
        params.put("top", "" + top);
        params.put("left", "" + left);
        params.put("width", "" + width);
        params.put("height", "" + height);

        // The "imageCropping" converter is defined in
        // OSGI-INF/extensions/conversions-contrib.xml
        Blob croppedBlob = ConversionUtils.convert("imageCropping",
                pictureBlob, params, targetFileName);
        croppedBlob.setMimeType(pictureBlob.getMimeType());

        if (title == null || title.isEmpty()) {
            title = "Crop-" + top + "-" + left + "-" + width + "x" + height;
        }
        
        MultiviewPicture mvp = inDoc.getAdapter(MultiviewPicture.class);
        PictureView view = mvp.getView(title);
        if(view != null) {
            mvp.removeView(title);
        }

        ImagingService imagingService = Framework.getService(ImagingService.class);
        ImageInfo info = imagingService.getImageInfo(croppedBlob);
        view = new PictureViewImpl();
        view.setTitle(title);
        view.setContent(croppedBlob);
        view.setFilename(croppedBlob.getFilename());
        view.setDescription(title);
        view.setTag(title);
        view.setHeight((int) height);
        view.setWidth((int) width);
        mvp.addView(view);

        inDoc = session.saveDocument(inDoc);

        return inDoc;
    }

}
