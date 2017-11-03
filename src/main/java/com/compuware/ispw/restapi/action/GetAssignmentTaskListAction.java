package com.compuware.ispw.restapi.action;

import java.util.Arrays;
import java.util.List;

import com.compuware.ispw.restapi.IspwRequestBean;
import com.compuware.ispw.restapi.WebhookToken;

public class GetAssignmentTaskListAction extends AbstractAction {

	public static String[] defaultProps = new String[] { assignmentId, level };
	public static String contextPath = "/ispw/{srid}/assignments/{assignmentId}/tasks?level={level}";

	@Override
	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody,
			WebhookToken webhookToken) {
		List<String> replaceTokens = Arrays.asList(defaultProps);
		return super.getIspwRequestBean(srid, ispwRequestBody, webhookToken, contextPath,
				replaceTokens);
	}

}
