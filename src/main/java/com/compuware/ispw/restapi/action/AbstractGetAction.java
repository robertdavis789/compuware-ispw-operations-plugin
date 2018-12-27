package com.compuware.ispw.restapi.action;

import java.io.PrintStream;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.compuware.ispw.restapi.HttpMode;
import com.compuware.ispw.restapi.IspwContextPathBean;
import com.compuware.ispw.restapi.IspwRequestBean;
import com.compuware.ispw.restapi.util.ReflectUtils;

/**
 * A generic rest GET ISPW action
 * 
 * @author Sam Zhou
 *
 */
public abstract class AbstractGetAction implements IAction {

	private PrintStream logger;

	public AbstractGetAction(PrintStream logger) {
		this.logger = logger;
	}
	
	public IspwRequestBean getIspwRequestBean(String srid, String ispwRequestBody, String contextPath, List<String> pathTokens) {
		
		IspwRequestBean bean = new IspwRequestBean();
		
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
					if (pathTokens.contains(name)) {
						path = path.replace("{" + name + "}", value);

						ReflectUtils.reflectSetter(ispwContextPathBean, name, value);
					}
				}
			}
		}

		bean.setContextPath(path);
		return bean;

	}

	public PrintStream getLogger() {
		return logger;
	}

	@Override
	public HttpMode getHttpMode()
	{
		return HttpMode.GET;
	}
}
