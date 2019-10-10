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
import java.io.PrintStream;
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
public class BuildAssignmentAction extends SetInfoPostAction implements IBuildAction
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
		return getIspwRequestBean(srid, ispwRequestBody, webhookToken, contextPath);
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

	/* (non-Javadoc)
	 * @see com.compuware.ispw.restapi.action.IBuildAction#getIspwRequestBean(java.lang.String, java.lang.String, com.compuware.ispw.restapi.WebhookToken, java.io.File)
	 */
	@Override
	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody, WebhookToken webhookToken,
			File buildDirectory)
	{
		System.out.println("getIspwRequestBean");
		ispwRequestBody = getRequestBody(ispwRequestBody, buildDirectory, this.getLogger());
		return getIspwRequestBean(srid, ispwRequestBody, webhookToken);
	}
}
