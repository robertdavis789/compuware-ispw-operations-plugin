/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2019 Compuware Corporation. All rights reserved.
 */
package com.compuware.ispw.restapi.action;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import com.compuware.ispw.cli.model.BuildParms;
import com.compuware.ispw.restapi.IspwRequestBean;
import com.compuware.ispw.restapi.WebhookToken;

/**
 * 
 */
public interface IBuildAction
{
	public static String BUILD_PARAM_FILE_NAME = "automaticBuildParams.txt"; //$NON-NLS-1$
	
	public default String getRequestBody(String ispwRequestBody, File buildDirectory, PrintStream logger)
	{
		logger.println("getRequestBody");
		Pattern pattern = Pattern.compile("^(?!#)(.+)?buildautomatically.+true(.+)?$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE); //$NON-NLS-1$
		Matcher matcher = pattern.matcher(ispwRequestBody);
		logger.println("given body = \n" + ispwRequestBody);
		if (matcher.matches())
		{
			logger.println("matcher matches");
			// if a line of the body contains "buildautomatically" and "true" case insensitive, and the line is not a comment.
			File parmFile = new File(buildDirectory, BUILD_PARAM_FILE_NAME);
			logger.println(parmFile.getAbsolutePath());
//			try
//			{
				String jsonString = "{\"containerId\" : \"PLAY002632\", \"taskLevel\" : \"DEV1\", \"taskIds\" : [\"7E39DD1E5D45\"]}";
			// String jsonString = FileUtils.readFileToString(parmFile, CharEncoding.UTF_8);
			BuildParms buildParms = BuildParms.parse(jsonString);
			if (buildParms != null)
			{
				StringBuilder requestBodyBuffer = new StringBuilder();
				if (buildParms.getContainerId() != null)
				{
					requestBodyBuffer.append("assignmentId = " + buildParms.getContainerId());
				}
				if (buildParms.getTaskLevel() != null)
				{
					requestBodyBuffer.append("\nlevel = " + buildParms.getTaskLevel());
				}
				if (buildParms.getReleaseId() != null)
				{
					requestBodyBuffer.append("\nreleaseId = " + buildParms.getReleaseId());
				}
				if (buildParms.getTaskIds() != null && !buildParms.getTaskIds().isEmpty())
				{
					requestBodyBuffer.append("\ntaskId = " + buildParms.getTaskIds().get(0));
				}
				ispwRequestBody = requestBodyBuffer.toString();
			}
				
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
		}
		logger.println("requestBody = \n" + ispwRequestBody);
		return ispwRequestBody;
	}

	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody, WebhookToken webhookToken, File buildDirectory);

}
