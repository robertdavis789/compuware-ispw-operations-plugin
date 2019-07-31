package com.compuware.ispw.git;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class RefMap
{
	private String containerPref = GitToIspwConstants.CONTAINER_PREF_PER_COMMIT;
	private String containerDesc = StringUtils.EMPTY;
	
	private String ispwLevel;

	public RefMap(String ispwLevel, String containerPref, String containerDesc)
	{
		this.ispwLevel = ispwLevel;
		
		this.containerPref = containerPref;
		this.containerDesc = containerDesc;
		
	}

	public String toString()
	{
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	/**
	 * @return the containerPref
	 */
	public String getContainerPref()
	{
		return containerPref;
	}

	/**
	 * @param containerPref
	 *            the containerPref to set
	 */
	public void setContainerPref(String containerPref)
	{
		this.containerPref = containerPref;
	}

	/**
	 * @return the containerDesc
	 */
	public String getContainerDesc()
	{
		return containerDesc;
	}

	/**
	 * @param containerDesc the containerDesc to set
	 */
	public void setContainerDesc(String containerDesc)
	{
		this.containerDesc = containerDesc;
	}
	
	/**
	 * @return the ispwLevel
	 */
	public String getIspwLevel()
	{
		return ispwLevel;
	}

	/**
	 * @param ispwLevel
	 *            the ispwLevel to set
	 */
	public void setIspwLevel(String ispwLevel)
	{
		this.ispwLevel = ispwLevel;
	}

}
