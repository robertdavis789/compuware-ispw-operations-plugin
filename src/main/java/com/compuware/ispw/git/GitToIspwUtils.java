package com.compuware.ispw.git;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Util;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;

public class GitToIspwUtils
{

	public static ListBoxModel buildStandardCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
			@AncestorInPath Item project)
	{
		List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
				StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
				Collections.<DomainRequirement> emptyList());

		StandardListBoxModel model = new StandardListBoxModel();

		model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

		for (StandardUsernamePasswordCredentials c : creds)
		{
			boolean isSelected = false;

			if (credentialsId != null)
			{
				isSelected = credentialsId.matches(c.getId());
			}

			String description = Util.fixEmptyAndTrim(c.getDescription());
			model.add(new Option(c.getUsername() + (description != null ? " (" + description + ")" : StringUtils.EMPTY), //$NON-NLS-1$ //$NON-NLS-2$
					c.getId(), isSelected));
		}

		return model;
	}
	
	public static ListBoxModel buildContainerPrefItems(@AncestorInPath Jenkins context, @QueryParameter String containerPref,
			@AncestorInPath Item project)
	{
		ListBoxModel model = new ListBoxModel();

		model.add(new Option(GitToIspwConstants.CONTAINER_PREF_PER_COMMIT, GitToIspwConstants.CONTAINER_PREF_PER_COMMIT));
		model.add(new Option(GitToIspwConstants.CONTAINER_PREF_PER_BRANCH, GitToIspwConstants.CONTAINER_PREF_PER_BRANCH));
		model.add(new Option(GitToIspwConstants.CONTAINER_PREF_CUSTOM, GitToIspwConstants.CONTAINER_PREF_CUSTOM));
		
		return model;
	}
}
