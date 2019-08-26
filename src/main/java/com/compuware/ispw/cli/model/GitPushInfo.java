package com.compuware.ispw.cli.model;

import java.io.Serializable;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class to keep track of information about a Git push that will be synched to ISPW.
 */
public class GitPushInfo implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String ref;
	private String fromHash;
	private String toHash;
	private ArrayList<String> successfulCommits = new ArrayList<>();
	private ArrayList<String> failedCommits = new ArrayList<>();
	private String ispwLevel;
	private String containerCreationPref;
	private String customDescription;
	private static Gson gson = new GsonBuilder().create();

	/**
	 * Constructor.
	 */
	public GitPushInfo()
	{

	}

	/**
	 * Constructor that initializes some information and defaults the successfulCommits and failedCommits list to empty.
	 * 
	 * @param ref
	 *            the Git branch name
	 * @param fromHash
	 *            the starting hash
	 * @param toHash
	 *            the ending hash
	 * @param ispwLevel
	 *            the ISPW level to load the changes into
	 * @param containerCreationPref
	 *            the container creation preference for this push
	 * @param customDescription
	 *            the custom description for creating a container (if applicable)
	 */
	public GitPushInfo(String ref, String fromHash, String toHash, String ispwLevel, String containerCreationPref,
			String customDescription)
	{
		this.ref = ref;
		this.fromHash = fromHash;
		this.toHash = toHash;
		this.ispwLevel = ispwLevel;
		this.containerCreationPref = containerCreationPref;
		this.customDescription = customDescription;
	}

	/**
	 * Constructor that initializes all information.
	 * 
	 * @param ref
	 *            the Git branch name
	 * @param fromHash
	 *            the starting hash
	 * @param toHash
	 *            the ending hash
	 * @param ispwLevel
	 *            the ISPW level to load the changes into
	 * @param containerCreationPref
	 *            the container creation preference for this push
	 * @param customDescription
	 *            the custom description for creating a container (if applicable)
	 * @param successfulCommits
	 *            the list of commit IDs that have been successfully synced
	 * @param failedCommits
	 *            the list of commit IDs that have been attempted to sync and were not successful
	 */
	public GitPushInfo(String ref, String refId, String fromHash, String toHash, String ispwLevel, String containerCreationPref,
			String customDescription, ArrayList<String> successfulCommits, ArrayList<String> failedCommits)
	{
		this(ref, fromHash, toHash, ispwLevel, containerCreationPref, customDescription);
		this.successfulCommits = successfulCommits;
		this.failedCommits = failedCommits;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fromHash == null) ? 0 : fromHash.hashCode());
		result = prime * result + ((toHash == null) ? 0 : toHash.hashCode());
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
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
		GitPushInfo other = (GitPushInfo) obj;
		if (toHash == null)
		{
			if (other.toHash != null)
				return false;
		}
		else if (!toHash.equals(other.toHash))
			return false;
		if (fromHash == null)
		{
			if (other.fromHash != null)
				return false;
		}
		else if (!fromHash.equals(other.fromHash))
			return false;
		if (ref == null)
		{
			if (other.ref != null)
				return false;
		}
		else if (!ref.equals(other.ref))
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

	/**
	 * Parses the given String into a GitPushInfo object
	 * 
	 * @param s
	 * @return
	 */
	public static GitPushInfo parse(String s)
	{
		return gson.fromJson(s, GitPushInfo.class);
	}

	/**
	 * @return the ref
	 */
	public String getRef()
	{
		return ref;
	}

	/**
	 * @param ref
	 *            the ref to set
	 */
	public void setRef(String ref)
	{
		this.ref = ref;
	}

	/**
	 * @return the fromHash
	 */
	public String getFromHash()
	{
		return fromHash;
	}

	/**
	 * @param fromHash
	 *            the fromHash to set
	 */
	public void setFromHash(String fromHash)
	{
		this.fromHash = fromHash;
	}

	/**
	 * @return the toHash
	 */
	public String getToHash()
	{
		return toHash;
	}

	/**
	 * @param toHash
	 *            the toHash to set
	 */
	public void setToHash(String toHash)
	{
		this.toHash = toHash;
	}

	/**
	 * @return the successfulCommits
	 */
	public ArrayList<String> getSuccessfulCommits()
	{
		return successfulCommits;
	}

	/**
	 * @param successfulCommits
	 *            the successfulCommits to set
	 */
	public void setSuccessfulCommits(ArrayList<String> successfulCommits)
	{
		this.successfulCommits = successfulCommits;
	}

	/**
	 * @return the failedCommits
	 */
	public ArrayList<String> getFailedCommits()
	{
		return failedCommits;
	}

	/**
	 * @param failedCommits
	 *            the failedCommits to set
	 */
	public void setFailedCommits(ArrayList<String> failedCommits)
	{
		this.failedCommits = failedCommits;
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

	/**
	 * @return the containerCreationPref
	 */
	public String getContainerCreationPref()
	{
		return containerCreationPref;
	}

	/**
	 * @param containerCreationPref
	 *            the containerCreationPref to set
	 */
	public void setContainerCreationPref(String containerCreationPref)
	{
		this.containerCreationPref = containerCreationPref;
	}

	/**
	 * @return the customDescription
	 */
	public String getCustomDescription()
	{
		return customDescription;
	}

	/**
	 * @param customDescription
	 *            the customDescription to set
	 */
	public void setCustomDescription(String customDescription)
	{
		this.customDescription = customDescription;
	}

	public void addSuccessfulCommit(String commitId)
	{
		if (successfulCommits != null)
		{
			successfulCommits.add(commitId);
		}
	}

	public void addFailedCommit(String commitId)
	{
		if (failedCommits != null)
		{
			failedCommits.add(commitId);
		}
	}

	public void removeAllFailedCommits()
	{
		if (failedCommits != null)
		{
			failedCommits.clear();
		}
	}
}
