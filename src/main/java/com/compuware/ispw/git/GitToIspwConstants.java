package com.compuware.ispw.git;

public class GitToIspwConstants
{
	public static String CONTAINER_PREF_PER_COMMIT = "per-commit";
	public static String CONTAINER_PREF_PER_BRANCH = "per-branch";
	public static String CONTAINER_PREF_CUSTOM = "custom";
	
	public static final String SCM_DOWNLOADER_CLI_BAT = "IspwCLI.bat";
	public static final String SCM_DOWNLOADER_CLI_SH = "IspwCLI.sh";
	
	public static final String ISPW_OPERATION_PARAM = "-operation";
	
	public static final String ISPW_SERVER_CONFIG_PARAM = "-ispwServerConfig";
	public static final String ISPW_SERVER_STREAM_PARAM = "-ispwServerStream";
	public static final String ISPW_SERVER_APP_PARAM = "-ispwServerApp";
	public static final String ISPW_SERVER_CHECKOUT_LEV_PARAM = "-ispwCheckoutLevel";
	
	public static final String GIT_REPO_URL_PARAM = "-gitRepoUrl";
	public static final String GIT_USERID_PARAM = "-gitUsername";
	public static final String GIT_PW_PARAM = "-gitPassword";
	public static final String GIT_REF_PARAM = "-gitBranch";
	public static final String GIT_HASH_PARAM = "-gitCommit";
}
