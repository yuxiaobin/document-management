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
package org.jahia.dm.thumbnails;

import java.io.File;

import javax.jcr.RepositoryException;

import org.jahia.dm.DocumentOperationException;
import org.jahia.services.Serviceable;
import org.jahia.services.content.JCRNodeWrapper;

/**
 * Video thumbnails generator API.
 * 
 * @author Cédric Mailleux
 * @author Sergiy Shyrkov
 */
public interface VideoThumbnailService extends Serviceable {

    /**
     * Returns <code>true</code> if a thumbnail can be generated for the supplied document, i.e. the thumbnail service is enabled and the
     * document format satisfies the requirements.
     * 
     * @param fileNode
     *            the video node to generate thumbnail for
     * @return <code>true</code> if a thumbnail can be generated for the supplied document, i.e. the thumbnail service is enabled and the
     *         document format satisfies the requirements
     * @throws RepositoryException
     */
    boolean canHandle(JCRNodeWrapper fileNode) throws RepositoryException;

    /**
     * Generates thumbnails for the specified video file node.
     * 
     * @param fileNode
     *            the node to generate thumbnails for
     * @param thumbnailName
     *            the name of the thumbnail node
     * @param offsetSeconds
     *            the input time offset in seconds. Specifying a positive offset means that the corresponding streams are delayed by offset
     *            seconds.
     * @param thumbnailSize
     *            the size of the generated thumbnail (e.g. 640x480)
     * @return <code>true</code> if the thumbnail was successfully created; returns <code>false</code> if e.g. the service is disabled or
     *         the document cannot be converted to the required format
     * @throws RepositoryException
     *             in case of repository operation error
     * @throws DocumentOperationException
     *             in case of a video transformation error
     */
    boolean createThumbnailForNode(JCRNodeWrapper fileNode, String thumbnailName,
            int offsetSeconds, String thumbnailSize) throws RepositoryException,
            DocumentOperationException;

    /**
     * Generates a JPG thumbnail image for the specified video file.
     * 
     * @param videoFile
     *            the video file to generate thumbnail for
     * @param outputFile
     *            the target thumbnail file descriptor
     * @param offsetSeconds
     *            the input time offset in seconds. Specifying a positive offset means that the corresponding streams are delayed by offset
     *            seconds.
     * @param size
     *            the target thumbnail size (e.g. 640x480)
     * @return the file descriptor for the generated thumbnail image
     * @throws DocumentOperationException
     *             if the document conversion exception occurs
     */
    boolean generateThumbnail(File videoFile, File outputFile, int offsetSeconds, String size)
            throws DocumentOperationException;

    /**
     * Generates a JPG thumbnail image for the specified video file.
     * 
     * @param videoFile
     *            the video file to generate thumbnail for
     * @param offsetSeconds
     *            the input time offset in seconds. Specifying a positive offset means that the corresponding streams are delayed by offset
     *            seconds.
     * @param size
     *            the target thumbnail size (e.g. 640x480)
     * @return the file descriptor for the generated thumbnail image
     * @throws DocumentOperationException
     *             if the document conversion exception occurs
     */
    File generateThumbnail(File videoFile, int offsetSeconds, String size)
            throws DocumentOperationException;
}