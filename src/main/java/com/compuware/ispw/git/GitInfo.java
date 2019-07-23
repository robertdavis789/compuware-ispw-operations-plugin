package com.compuware.ispw.git;

import java.util.StringTokenizer;

public class GitInfo
{
	private String ref;
	private String refId;
	private String hash;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
		result = prime * result + ((refId == null) ? 0 : refId.hashCode());
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
		GitInfo other = (GitInfo) obj;
		if (hash == null)
		{
			if (other.hash != null)
				return false;
		}
		else if (!hash.equals(other.hash))
			return false;
		if (ref == null)
		{
			if (other.ref != null)
				return false;
		}
		else if (!ref.equals(other.ref))
			return false;
		if (refId == null)
		{
			if (other.refId != null)
				return false;
		}
		else if (!refId.equals(other.refId))
			return false;
		return true;
	}

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
