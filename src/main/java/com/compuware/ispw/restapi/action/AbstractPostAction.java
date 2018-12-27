package com.compuware.ispw.restapi.action;

import java.io.PrintStream;
import org.apache.commons.lang3.StringUtils;
import com.compuware.ispw.restapi.HttpMode;
import com.compuware.ispw.restapi.IspwContextPathBean;
import com.compuware.ispw.restapi.IspwRequestBean;
import com.compuware.ispw.restapi.JsonProcessor;
import com.compuware.ispw.restapi.util.ReflectUtils;

/**
 * A generic rest POST ISPW action
 * @author Sam Zhou
 *
 */
public abstract class AbstractPostAction implements IAction {

	private PrintStream logger;

	public AbstractPostAction(PrintStream logger) {
		this.logger = logger;
	}
	
	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody,
			String contextPath, Object jsonObject) {

		IspwRequestBean bean = new IspwRequestBean();
		bean.setJsonObject(jsonObject);
		
		IspwContextPathBean ispwContextPathBean = new IspwContextPathBean();
		ispwContextPathBean.setSrid(srid);
		bean.setIspwContextPathBean(ispwContextPathBean);
		
		String path = contextPath.replace("{srid}", srid);

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
					ReflectUtils.reflectSetter(jsonObject, name, value);
				}
			}
		}

		bean.setContextPath(path);

		JsonProcessor jsonGenerator = new JsonProcessor();
		String jsonRequest = jsonGenerator.generate(jsonObject);
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
