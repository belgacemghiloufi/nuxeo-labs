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
 *     Thibaud Arguillere
 */
package org.nuxeo.labs.operations.images;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters.ParameterValue;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.api.Framework;

import com.google.common.io.Files;

/**
 * Use ImageMagick <code>montage</code> to build a sheet from the images, with misc. parameters.
 * <p>
 * please refer to ImageMagick documentation for parameters meaning and formats.
 * <p>
 * The following may require more explanation:
 * <ul>
 * <li>label: A pattern. %f for the file name, %w for the width, ... For example, to draw the title and the size below:
 * "%f\n#wx#h"<br/>
 * Default value is "%f" (see "useDocTitle" below)</li>
 * <li>define: ImageMagick keeps all images in cache. if the list has a lot of big images, it will fails. This parameter
 * allows to reduce the size of the image in memory.<br>
 * Default value is "jpeg:size=150x150"</li>
 * <li>geometry: The size of each thumb, expressed as a String, regular ImageMagic geometry parameter. Default value is
 * "150x150+20+20" which means a thumb is a rectangle of 150 pixel, and there is a margin of 20 pixels</li>
 * <li></li>
 * </ul>
 * <p>
 * The <code>view</code> is the <Code>PictureView</code> t use for building the final thumbnail. We use "Medium" by
 * default, but if the bview is not found (removed after configuration for example), the plug-in uses the original
 * "file:content" binary.
 * <p>
 * it is also possible to contribute your own command-line and use this class to run it. You can even declare/pass more
 * parameters. There are 2 parameters that are required by the plug-in and must be used exactly as the following:
 * <ul>
 * <li>They must be the last two parameters</li>
 * <li>They must exactly be: <code> @#{listFilePath} #{targetFilePath}</code> (plesae notice the @ sign)</li>
 * </ul>
 *
 * @since 8.2
 */
public class ImagesSheetBuilder {

    private static final Log log = LogFactory.getLog(ImagesSheetBuilder.class);

    public static final String DEFAULT_COMMAND = "IM-montage";

    public static final String DEFAULT_LABEL = "%f";

    public static final String NO_LABEL = "NO_LABEL";

    // Format is "geometry". nbPerRowsxnbPerColumns
    // To just have 4 per rows, pass "4"
    public static final String DEFAULT_TILE = "0";

    public static final String DEFAULT_FONT = "Helvetica";

    public static final int DEFAULT_FONT_SIZE = 12;

    public static final String DEFAULT_BACKGROUND = "white";

    public static final String DEFAULT_FILL = "black";

    public static final String DEFAULT_DEFINE = "jpeg:size=150x150>";

    public static final String DEFAULT_GEOMETRY = "150x150>+20+20";

    public static final String RESULT_FILE_NAME = "thumbnails-sheet.jpg";

    public static final String RESULT_MIMETYPE = "image/jpeg";

    public static final String DEFAULT_VIEW = "Medium";

    public static final boolean DEFAULT_USE_DOC_TITLE = false;

    protected String label = DEFAULT_LABEL;

    protected String font = DEFAULT_FONT;

    protected int fontSize = DEFAULT_FONT_SIZE;

    protected String background = DEFAULT_BACKGROUND;

    protected String fill = DEFAULT_FILL;

    protected String define = DEFAULT_DEFINE;

    protected String geometry = DEFAULT_GEOMETRY;

    protected String view = DEFAULT_VIEW;

    protected boolean useDocTitle = DEFAULT_USE_DOC_TITLE;

    protected String tile = DEFAULT_TILE;

    protected DocumentModelList docs = null;

    protected BlobList blobs = null;

    protected String command = DEFAULT_COMMAND;

    protected static ThumbnailService thumbnailService = null;

    public ImagesSheetBuilder(DocumentModelList inDocs) {
        docs = inDocs;
    }

    public ImagesSheetBuilder(BlobList inBlobs) {
        blobs = inBlobs;
    }

    protected ThumbnailService getThumbnailService() {
        // Not 100% threadsafe, but it's ok in this context
        if (thumbnailService == null) {
            thumbnailService = Framework.getService(ThumbnailService.class);
        }

        return thumbnailService;
    }

    public Blob build() throws IOException, CommandNotAvailable, NuxeoException {
        return build(null);
    }

    public Blob build(CmdParameters moreParameters) throws IOException, CommandNotAvailable, NuxeoException {

        Blob result = null;

        // Error check
        if (docs == null && blobs == null) {
            return null;
        }
        if (docs != null && docs.size() < 1) {
            return null;
        }
        if (blobs != null && blobs.size() < 1) {
            return null;
        }

        // Priority to docs for compatibility
        if (docs != null) {
            blobs = new BlobList();
            ;
            Blob blob;
            boolean useView = StringUtils.isNotBlank(view);
            for (DocumentModel doc : docs) {
                blob = null;
                if (doc.hasFacet("Picture")) {
                    // Get the blob of the view or the whole content
                    if (useView) {
                        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
                        if (mvp != null) {
                            PictureView pv = mvp.getView(view);
                            if (pv != null) {
                                blob = pv.getBlob();
                            }
                        }
                    }

                    if (blob == null) {
                        blob = (Blob) doc.getPropertyValue("file:content");
                    }

                }
                if (blob == null) {
                    blob = getThumbnailService().getThumbnail(doc, doc.getCoreSession());
                }

                if (useDocTitle) {
                    blob.setFilename(doc.getTitle());
                }

                blobs.add(blob);

            }
        } // if (docs != null)

        // Prepare each blob
        // Duplicate the images in a temp folder
        // and build a list of path, ordered as the doc list
        File tempDir = Files.createTempDir();
        File f;
        String fileList = "";
        for (Blob b : blobs) {
            // Duplicate
            if (b != null) {
                f = new File(tempDir, b.getFilename());
                fileList += "\"" + f.getAbsolutePath() + "\"\n";

                b.transferTo(f);
            }
        }

        // Create the file to be used by ImageMagic to get the files
        File listOfFiles = new File(tempDir, "list.txt");
        FileUtils.writeStringToFile(listOfFiles, fileList, "UTF-8");

        // Call the command line
        // * Create a temp blob, handled by Nuxeo
        result = Blobs.createBlobWithExtension(".jpg");
        String outputFilePath = result.getFile().getAbsolutePath();

        // * Setup up the command line parameters (see "IM-montage" in conversions.xlml)
        CmdParameters params = new CmdParameters();
        if (label != null && label.equals(NO_LABEL)) {
            label = "";
        }
        params.addNamedParameter("label", label);
        params.addNamedParameter("font", font);
        params.addNamedParameter("fontSize", "" + fontSize);
        params.addNamedParameter("background", background);
        params.addNamedParameter("fill", fill);
        params.addNamedParameter("define", define);
        params.addNamedParameter("geometry", geometry);
        params.addNamedParameter("tile", tile);
        params.addNamedParameter("listFilePath", listOfFiles);
        params.addNamedParameter("targetFilePath", outputFilePath);

        // * Add optional parameters (when used with another commandline)
        // We accept only String as ParameterValue (not File or String List)
        if (moreParameters != null) {
            Map<String, ParameterValue> more = moreParameters.getParameters();
            for (Entry<String, ParameterValue> entry : more.entrySet()) {
                params.addNamedParameter(entry.getKey(), entry.getValue().getValue());
            }
        }

        // * Run
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        ExecResult clResult = cles.execCommand(command, params);

        // * Handle errors
        // It may happen ImageMagick returns 1 while the operation went well. We can't rely on clResult
        // So we test if we do have a result and handle an error if we don't
        if (!result.getFile().exists() || result.getFile().length() == 0) {
            result = null;
            log.error("Failed to build the Images Sheet: \n" + "Command Line: " + clResult.getCommandLine() + "\n"
                    + "Result code: " + clResult.getReturnCode() + "\n" + "Error: " + clResult.getError());
            throw new NuxeoException("Failed to build the Images Sheet");
        } else {
            result.setMimeType(RESULT_MIMETYPE);
            result.setFilename(RESULT_FILE_NAME);
        }

        // Cleanup the temp folder now
        FileUtils.deleteDirectory(tempDir);

        return result;

    }

    public String getCommand() {
        return command;
    }

    /**
     * Set the contributed command line to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setCommand(String value) {
        command = StringUtils.isBlank(value) ? DEFAULT_COMMAND : value;
        return this;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Set the label to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setLabel(String value) {
        label = StringUtils.isBlank(value) ? DEFAULT_LABEL : value;
        return this;
    }

    public String getFont() {
        return font;
    }

    /**
     * Set the font to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setFont(String value) {
        font = StringUtils.isBlank(value) ? DEFAULT_FONT : value;
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    /**
     * Set the font size to use. If null or < 1, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setFontSize(int value) {
        fontSize = value <= 0 ? DEFAULT_FONT_SIZE : value;
        return this;
    }

    /**
     * Set the font size to use. If null or < 1, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setFontSize(Long value) {
        fontSize = value == null || value.intValue() <= 0 ? DEFAULT_FONT_SIZE : value.intValue();
        return this;
    }

    public String getBackground() {
        return background;
    }

    /**
     * Set the background color to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setBackground(String value) {
        background = StringUtils.isBlank(value) ? DEFAULT_BACKGROUND : value;
        return this;
    }

    public String getFill() {
        return fill;
    }

    /**
     * Set the fill color to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setFill(String value) {
        fill = StringUtils.isBlank(value) ? DEFAULT_FILL : value;
        return this;
    }

    public String getDefine() {
        return define;
    }

    /**
     * Set the "define" to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setDefine(String value) {
        define = StringUtils.isBlank(value) ? DEFAULT_DEFINE : value;
        return this;
    }

    public String getGeometry() {
        return geometry;
    }

    /**
     * Set the geometry of each thumb. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setGeometry(String value) {
        geometry = StringUtils.isBlank(value) ? DEFAULT_GEOMETRY : value;
        return this;
    }

    public String getTile() {
        return tile;
    }

    /**
     * Set the tile (nb colums) to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setTile(String value) {
        tile = StringUtils.isBlank(value) ? DEFAULT_TILE : value;
        return this;
    }

    public String getView() {
        return view;
    }

    /**
     * Set the picture view to use. If empty or null, use the default value.
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setView(String value) {
        view = StringUtils.isBlank(value) ? DEFAULT_VIEW : value;
        return this;
    }

    public boolean useDocTitle() {
        return useDocTitle;
    }

    /**
     * Tell the builder to use the Document titles instead of the file names
     *
     * @param value
     * @return the <code>this</object>
     * @since 8.2
     */
    public ImagesSheetBuilder setUseDocTitle(boolean value) {
        useDocTitle = value;
        return this;
    }

}
