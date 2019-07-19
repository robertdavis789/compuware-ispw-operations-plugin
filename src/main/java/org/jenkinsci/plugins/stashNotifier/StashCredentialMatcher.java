package org.jenkinsci.plugins.stashNotifier;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A very simple matcher to ensure we only show username/password or certificate credentials
 */
public class StashCredentialMatcher implements CredentialsMatcher {
	private static final long serialVersionUID = -15697644398715528L;

	public boolean matches(@NonNull Credentials credentials) {
        return (credentials instanceof CertificateCredentials) || (credentials instanceof UsernamePasswordCredentials);
    }
}
