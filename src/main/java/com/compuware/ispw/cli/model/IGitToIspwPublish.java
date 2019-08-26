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

/**
 * 
 */
public interface IGitToIspwPublish
{
	/**
	 * @return the gitRepoUrl
	 */
	public String getGitRepoUrl();

	/**
	 * @return the gitCredentialsId
	 */
	public String getGitCredentialsId();

	/**
	 * @return the connectionId
	 */
	public String getConnectionId();

	/**
	 * @return the credentialsId
	 */
	public String getCredentialsId();

	/**
	 * @return the runtimeConfig
	 */
	public String getRuntimeConfig();

	/**
	 * @return the stream
	 */
	public String getStream();

	/**
	 * @return the app
	 */
	public String getApp();
}
