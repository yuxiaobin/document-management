/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.dm.thumbnails.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import net.coobird.thumbnailator.Thumbnails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.dm.DocumentOperationException;
import org.jahia.dm.thumbnails.DocumentThumbnailService;
import org.jahia.dm.thumbnails.DocumentThumbnailServiceAware;
import org.jahia.dm.thumbnails.PDF2ImageConverter;
import org.jahia.dm.thumbnails.PDF2ImageConverterAware;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.templates.TemplatePackageApplicationContextLoader.ContextInitializedEvent;
import org.jahia.services.transform.DocumentConverterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationListener;

/**
 * The document thumbnail generation service.
 * 
 * @author Sergiy Shyrkov
 */
public class DocumentThumbnailServiceImpl implements DocumentThumbnailService,
        ApplicationListener<ContextInitializedEvent>, PDF2ImageConverterAware {

    private static final Logger logger = LoggerFactory
            .getLogger(DocumentThumbnailServiceImpl.class);

    private DocumentConverterService documentConverter;

    private boolean enabled = true;

    private PDF2ImageConverter pdf2ImageConverter;

    private String[] supportedDocumentFormats;

    private boolean usePNGForThumbnailImage = true;

    public boolean canHandle(JCRNodeWrapper fileNode) throws RepositoryException {
        if (!isEnabled() || supportedDocumentFormats == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Thumbnails service is disabled"
                                + (supportedDocumentFormats == null ? " as no supported document formats are configured"
                                        : "") + ". Skip converting node {}", fileNode.getPath());
            }
            return false;
        }

        return fileNode.isNodeType("nt:file")
                && JCRContentUtils.isMimeTypeGroup(fileNode.getFileContent().getContentType(),
                        supportedDocumentFormats);
    }

    public boolean createThumbnail(JCRNodeWrapper fileNode, String thumbnailName, int thumbnailSize)
            throws RepositoryException, DocumentOperationException {
        if (!canHandle(fileNode)) {
            return false;
        }

        long timer = System.currentTimeMillis();

        JCRNodeWrapper thumbNode = null;

        BufferedImage image = null;
        BufferedImage thumbnail = null;
        try {
            image = getImageOfFirstPage(fileNode);

            if (image != null) {
                thumbnail = Thumbnails.of(image).size(thumbnailSize, thumbnailSize)
                        .asBufferedImage();
                thumbNode = storeThumbnailNode(fileNode, thumbnail, thumbnailName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Generated thumbnail {} for node {} in {} ms", new Object[] {
                            thumbNode.getPath(), fileNode.getPath(),
                            (System.currentTimeMillis() - timer) });
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (image != null) {
                image.flush();
            }
            if (thumbnail != null) {
                thumbnail.flush();
            }
        }

        return thumbNode != null;
    }

    public BufferedImage getImageOfFirstPage(JCRNodeWrapper fileNode) throws RepositoryException,
            DocumentOperationException {
        BufferedImage image = null;

        long timer = System.currentTimeMillis();
        String sourceContentType = fileNode.getFileContent().getContentType();
        InputStream pdfInputStream = null;
        File pdfFile = null;
        try {
            if (JCRContentUtils.isMimeTypeGroup(sourceContentType, "pdf")) {
                pdfInputStream = fileNode.getFileContent().downloadFile();
            } else {
                if (documentConverter == null || !documentConverter.isEnabled()) {
                    logger.info("Document conversion service is not enabled."
                            + " Cannot convert node {} into a PDF. Skip generating image.",
                            fileNode.getPath());
                    return null;
                } else {
                    long timerPdf = System.currentTimeMillis();
                    File inFile = null;
                    try {
                        inFile = File.createTempFile("doc-thumbnail-source", null);
                        JCRContentUtils.downloadFileContent(fileNode, inFile);
                        pdfFile = documentConverter.convert(inFile, sourceContentType,
                                "application/pdf");
                        pdfInputStream = new FileInputStream(pdfFile);
                    } catch (IOException e) {
                        throw new DocumentOperationException(
                                "Error occurred trying to generate an image for the first page of the node {}"
                                        + fileNode.getPath(), e);
                    } finally {
                        FileUtils.deleteQuietly(inFile);

                        if (pdfInputStream != null && logger.isDebugEnabled()) {
                            logger.debug("Converted document {} into a PDF document in {} ms",
                                    fileNode.getPath(), System.currentTimeMillis() - timerPdf);
                        }
                    }
                }
            }

            if (pdfInputStream != null) {
                image = pdf2ImageConverter.getImageOfPage(pdfInputStream, 0);
            }
        } finally {
            IOUtils.closeQuietly(pdfInputStream);
            FileUtils.deleteQuietly(pdfFile);

            if (image != null && logger.isDebugEnabled()) {
                logger.debug(
                        "Generated an image for the first page of the document node {} in {} ms",
                        fileNode.getPath(), System.currentTimeMillis() - timer);
            }
        }

        return image;
    }

    public boolean isEnabled() {
        return enabled && pdf2ImageConverter != null && pdf2ImageConverter.isEnabled();
    }

    public void onApplicationEvent(ContextInitializedEvent event) {
        for (DocumentThumbnailServiceAware bean : BeanFactoryUtils.beansOfTypeIncludingAncestors(
                event.getContext(), DocumentThumbnailServiceAware.class).values()) {
            bean.setDocumentThumbnailService(this);
        }
    }

    public void setDocumentConverter(DocumentConverterService documentConverter) {
        this.documentConverter = documentConverter;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPDF2ImageConverter(PDF2ImageConverter service) {
        this.pdf2ImageConverter = service;
    }

    public void setSupportedDocumentFormats(String[] supportedDocumentFormats) {
        this.supportedDocumentFormats = supportedDocumentFormats;
    }

    public void setUsePNGForThumbnailImage(boolean usePNGForThumbnailImage) {
        this.usePNGForThumbnailImage = usePNGForThumbnailImage;
    }

    protected JCRNodeWrapper storeThumbnailNode(JCRNodeWrapper fileNode, BufferedImage thumbnail,
            String thumbnailName) throws RepositoryException, IOException {
        JCRNodeWrapper node = null;

        fileNode.getSession().checkout(fileNode);

        try {
            node = fileNode.getNode(thumbnailName);
        } catch (PathNotFoundException e) {
            node = fileNode.addNode(thumbnailName, Constants.JAHIANT_RESOURCE);
            node.addMixin(Constants.JAHIAMIX_IMAGE);
        }

        if (node.hasProperty(Constants.JCR_DATA)) {
            node.getProperty(Constants.JCR_DATA).remove();
        }

        Binary b = null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream(16 * 1024);
            ImageIO.write(thumbnail, usePNGForThumbnailImage ? "png" : "jpeg", os);
            b = new BinaryImpl(os.toByteArray());
            node.setProperty(Constants.JCR_DATA, b);
        } finally {
            os = null;
            if (b != null) {
                b.dispose();
            }
        }
        node.setProperty("j:width", thumbnail.getWidth());
        node.setProperty("j:height", thumbnail.getHeight());
        node.setProperty(Constants.JCR_MIMETYPE, usePNGForThumbnailImage ? "image/png"
                : "image/jpeg");
        Calendar lastModified = Calendar.getInstance();
        node.setProperty(Constants.JCR_LASTMODIFIED, lastModified);
        fileNode.setProperty(Constants.JCR_LASTMODIFIED, lastModified);

        return node;
    }
}