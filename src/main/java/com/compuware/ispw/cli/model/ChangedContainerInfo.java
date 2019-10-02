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
package com.compuware.ispw.cli.model;

import java.io.Serializable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 */
public class ChangedContainerInfo implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static Gson gson = new GsonBuilder().create();
	private String containerId;
	private String taskLevel;

	public ChangedContainerInfo(String containerId, String taskLevel)
	{
		this.containerId = containerId;
		this.taskLevel = taskLevel;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containerId == null) ? 0 : containerId.hashCode());
		result = prime * result + ((taskLevel == null) ? 0 : taskLevel.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChangedContainerInfo other = (ChangedContainerInfo) obj;
		if (containerId == null)
		{
			if (other.containerId != null)
				return false;
		}
		else if (!containerId.equals(other.containerId))
			return false;
		if (taskLevel == null)
		{
			if (other.taskLevel != null)
				return false;
		}
		else if (!taskLevel.equals(other.taskLevel))
			return false;

		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return gson.toJson(this);
	}

	public static ChangedContainerInfo parse(String s)
	{
		return gson.fromJson(s, ChangedContainerInfo.class);
	}

	/**
	 * @return the containerId
	 */
	public String getContainerId()
	{
		return containerId;
	}

	/**
	 * @param containerId
	 *            the containerId to set
	 */
	public void setContainerId(String containerId)
	{
		this.containerId = containerId;
	}

	/**
	 * @return the taskLevel
	 */
	public String getTaskLevel()
	{
		return taskLevel;
	}

	/**
	 * @param taskLevel
	 *            the taskLevel to set
	 */
	public void setTaskLevel(String taskLevel)
	{
		this.taskLevel = taskLevel;
	}

}
