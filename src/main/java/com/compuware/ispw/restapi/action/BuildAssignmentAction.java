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

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.compuware.ispw.model.rest.BuildResponse;
import com.compuware.ispw.restapi.Constants;
import com.compuware.ispw.restapi.IspwContextPathBean;
import com.compuware.ispw.restapi.IspwRequestBean;
import com.compuware.ispw.restapi.JsonProcessor;
import com.compuware.ispw.restapi.WebhookToken;
import com.compuware.ispw.restapi.util.RestApiUtils;

/**
 * Action to build an assignment
 */
public class BuildAssignmentAction extends SetInfoPostAction
{
	private static final String[] defaultProps = new String[]{assignmentId, level, runtimeConfiguration};

	private static final String contextPath = "/ispw/{srid}/assignments/{assignmentId}/tasks/build?level={level}"; //$NON-NLS-1$

	public static String getDefaultProps()
	{
		return RestApiUtils.join(Constants.LINE_SEPARATOR, defaultProps, true);
	}

	public BuildAssignmentAction(PrintStream logger)
	{
		super(logger);
	}

	@Override
	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody, WebhookToken webhookToken)
	{
		Pattern pattern = Pattern.compile("^(?!#).+\\bbuildautomatically\\b.+$", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher matcher= pattern.matcher(ispwRequestBody);
		if (matcher.matches())
		{
			// if the request body contains "buildautomatically" and the line is not a comment, remove that line.
			ispwRequestBody = matcher.replaceAll("");
		}
		IspwRequestBean bean = getIspwRequestBean(srid, ispwRequestBody, webhookToken, contextPath);
		if (matcher.matches())
		{
			
		}
		
		return bean;
	}

	@SuppressWarnings("nls")
	@Override
	public void startLog(PrintStream logger, IspwContextPathBean ispwContextPathBean, Object jsonObject)
	{
		logger.println("The build process started for assignment " + ispwContextPathBean.getAssignmentId() + " at level "
				+ ispwContextPathBean.getLevel());
	}

	@SuppressWarnings("nls")
	@Override
	public Object endLog(PrintStream logger, IspwRequestBean ispwRequestBean, String responseJson)
	{
		BuildResponse buildResp = new JsonProcessor().parse(responseJson, BuildResponse.class);
		logger.println("Set " + buildResp.getSetId() + " created to build tasks in assignment "
				+ ispwRequestBean.getIspwContextPathBean().getAssignmentId());

		return buildResp;
	}
}
