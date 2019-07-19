package com.compuware.ispw.git;

import java.util.StringTokenizer;

public class GitInfo
{
	private String ref;
	private String refId;
	private String hash;

	public GitInfo(String ref, String refId, String hash)
	{
		this.ref = ref;
		this.refId = refId;
		this.hash = hash;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return ref + "|" + refId + "|" + hash;
	}

	public static GitInfo parse(String s)
	{
		StringTokenizer tokenizer = new StringTokenizer(s, "|");
		String ref = tokenizer.nextToken();
		String refId = tokenizer.nextToken();
		String hash = tokenizer.nextToken();

		return new GitInfo(ref, refId, hash);
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
	 * @return the refId
	 */
	public String getRefId()
	{
		return refId;
	}

	/**
	 * @param refId
	 *            the refId to set
	 */
	public void setRefId(String refId)
	{
		this.refId = refId;
	}

	/**
	 * @return the hash
	 */
	public String getHash()
	{
		return hash;
	}

	/**
	 * @param hash
	 *            the hash to set
	 */
	public void setHash(String hash)
	{
		this.hash = hash;
	}

}
