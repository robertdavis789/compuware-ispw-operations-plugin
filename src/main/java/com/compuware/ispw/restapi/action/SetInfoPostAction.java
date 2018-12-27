package com.compuware.ispw.restapi.action;

import java.io.PrintStream;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import com.compuware.ces.communications.service.data.EventCallback;
import com.compuware.ces.model.BasicAuthentication;
import com.compuware.ces.model.HttpHeader;
import com.compuware.ispw.model.rest.SetInfo;
import com.compuware.ispw.restapi.HttpMode;
import com.compuware.ispw.restapi.IspwContextPathBean;
import com.compuware.ispw.restapi.IspwRequestBean;
import com.compuware.ispw.restapi.JsonProcessor;
import com.compuware.ispw.restapi.WebhookToken;
import com.compuware.ispw.restapi.util.ReflectUtils;
import com.compuware.ispw.restapi.util.RestApiUtils;

/**
 * A generic set post action
 * 
 * @author Sam Zhou
 *
 */
public abstract class SetInfoPostAction implements IAction {

	private PrintStream logger;

	public SetInfoPostAction(PrintStream logger) {
		this.logger = logger;
	}
	
	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody,
			WebhookToken webhookToken, String contextPath) {

		IspwRequestBean bean = new IspwRequestBean();

		IspwContextPathBean ispwContextPathBean = new IspwContextPathBean();
		ispwContextPathBean.setSrid(srid);
		bean.setIspwContextPathBean(ispwContextPathBean);
		
		String path = contextPath.replace("{srid}", srid);
		SetInfo setInfo = new SetInfo();
		bean.setJsonObject(setInfo);

		boolean hasEvent = false;
		ArrayList<EventCallback> events = new ArrayList<EventCallback>();
		EventCallback event = new EventCallback();
		events.add(event);

		String[] lines = ispwRequestBody.split("\n");
		for (String line : lines) {
			line = StringUtils.trimToEmpty(line);
			
			if(line.startsWith("#")) {
				continue;
			}
			
			int indexOfEqualSign = line.indexOf("=");
			if (indexOfEqualSign != -1) {
				String name = StringUtils.trimToEmpty(line.substring(0, indexOfEqualSign));
				String value =
						StringUtils
								.trimToEmpty(line.substring(indexOfEqualSign + 1, line.length()));

				if (StringUtils.isNotBlank(value)) {
					if (name.equals(assignmentId)) {
						path = path.replace("{" + assignmentId + "}", value);
						ispwContextPathBean.setAssignmentId(value);
					} else if(name.equals(releaseId)) {
						path = path.replace("{" + releaseId + "}", value);
						ispwContextPathBean.setReleaseId(value);
					} else if (name.equals(level)) {
						path = path.replace("{" + level + "}", value);
						ispwContextPathBean.setLevel(value);
					} else if (name.equals(mname)) {
						path = path.replace("{" + mname + "}", value);
						ispwContextPathBean.setMname(value);
					} else if (name.equals(mtype)) {
						path = path.replace("{" + mtype + "}", value);
						ispwContextPathBean.setMtype(value);
					} else if (name.equals(httpHeaders)) {
						ArrayList<HttpHeader> httpHeaders = RestApiUtils.toHttpHeaders(value);
						if (!httpHeaders.isEmpty()) {
							setInfo.setHttpHeaders(httpHeaders);
						}
					} else if (name.equals(credentials)) {
						BasicAuthentication auth = RestApiUtils.toBasicAuthentication(value);
						if (auth != null) {
							setInfo.setCredentials(auth);
						}
					} else if (name.equals(eventsName)) {
						hasEvent = true;
						event.setName(value);
					} else if (name.equals(eventsMethod)) {
						hasEvent = true;
						event.setMethod(value);
					} else if (name.equals(eventsBody)) {
						hasEvent = true;
						event.setBody(value);
					} else if (name.equals(eventsHttpHeaders)) {
						ArrayList<HttpHeader> httpHeaders = RestApiUtils.toHttpHeaders(value);
						if (!httpHeaders.isEmpty()) {
							hasEvent = true;
							event.setHttpHeaders(httpHeaders);
						}
					} else if (name.equals(eventsCredentials)) {
						BasicAuthentication auth = RestApiUtils.toBasicAuthentication(value);
						if (auth != null) {
							hasEvent = true;
							event.setCredentials(auth);
						}
					} else {
						ReflectUtils.reflectSetter(setInfo, name, value); // set the rest of the
																			// SetInfo fields using
																			// reflection
					}

				}
			}
		}

		if (webhookToken != null && hasEvent) {
			event.setUrl(webhookToken.getURL());
			setInfo.setEventCallbacks(events);
		}
		
		//if level/mname/mtype not set, remove them from query string
		path = path.replace("level={level}", StringUtils.EMPTY);
		path = path.replace("&mname={mname}", StringUtils.EMPTY);
		path = path.replace("&mtype={mtype}", StringUtils.EMPTY);
		
		bean.setContextPath(path);

		JsonProcessor jsonGenerator = new JsonProcessor();
		String jsonRequest = jsonGenerator.generate(setInfo);
		bean.setJsonRequest(jsonRequest);
		return bean;

	}

	public PrintStream getLogger() {
		return logger;
	}

	@Override
	public HttpMode getHttpMode()
	{
		return HttpMode.POST;
	}
	
}
