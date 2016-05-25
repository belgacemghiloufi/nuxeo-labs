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

package org.nuxeo.labs.images;

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;

/**
 * Receives a list of documents, outputs a blob, a JPEG Images Sheet. Different parameters allow to setup the output.
 * <p>
 * The default command uses ImageMagick <code>montage</code> commade: See its documentation for more details on the
 * parameters.
 * <p>
 * Special value for label: "NO_LABEL" means, well, no label at all
 * 
 * @since 8.2
 */
@Operation(id = ImagesSheetBuilderOp.ID, category = Constants.CAT_CONVERSION, label = "Images Sheet from Documents", description = "Build an image sheet from the input documents. Ignore documents that are do not have the Picture facet. Outputs the resulting image (always a jpeg). See ImageMagick montage command line for details about the parameters")
public class ImagesSheetBuilderOp {

    public static final String ID = "ImagesSheet.Build";

    @Param(name = "tile", required = false, values = { ImagesSheetBuilder.DEFAULT_TILE })
    protected String tile;

    @Param(name = "labelPattern", required = false, values = { ImagesSheetBuilder.DEFAULT_LABEL })
    protected String labelPattern;

    @Param(name = "backgroundColor", required = false, values = { ImagesSheetBuilder.DEFAULT_BACKGROUND })
    protected String backgroundColor;

    @Param(name = "fillColor", required = false, values = { ImagesSheetBuilder.DEFAULT_FILL })
    protected String fillColor;

    @Param(name = "font", required = false, values = { ImagesSheetBuilder.DEFAULT_FONT })
    protected String font;

    @Param(name = "fontSize", required = false)
    protected Long fontSize;

    @Param(name = "define", required = false, values = { ImagesSheetBuilder.DEFAULT_DEFINE })
    protected String define;

    @Param(name = "geometry", required = false, values = { ImagesSheetBuilder.DEFAULT_GEOMETRY })
    protected String geometry;

    @Param(name = "imageViewToUse", required = false, values = { ImagesSheetBuilder.DEFAULT_VIEW })
    protected String imageViewToUse;

    @Param(name = "useDocTitle", required = false, values = { "false" })
    protected boolean useDocTitle = false;

    @Param(name = "commandLine", required = false)
    protected String commandLine;

    @OperationMethod
    public Blob run(DocumentModelList input) throws NuxeoException, IOException, CommandNotAvailable {

        ImagesSheetBuilder isb = new ImagesSheetBuilder(input);

        isb.setTile(tile)
           .setLabel(labelPattern)
           .setBackground(backgroundColor)
           .setFill(fillColor)
           .setFont(font)
           .setFontSize(fontSize)
           .setDefine(define)
           .setGeometry(geometry)
           .setView(imageViewToUse)
           .setUseDocTitle(useDocTitle)
           .setCommand(commandLine);

        Blob result = isb.build();

        return result;
    }

}
