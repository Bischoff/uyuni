/*
 * Copyright (c) 2018 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.redhat.rhn.domain.scc;

import com.redhat.rhn.domain.BaseDomainHelper;
import com.redhat.rhn.domain.channel.ContentSource;
import com.redhat.rhn.domain.credentials.BaseCredentials;
import com.redhat.rhn.domain.credentials.Credentials;
import com.redhat.rhn.domain.credentials.RemoteCredentials;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * This is a SUSE repository as parsed from JSON coming in from SCC.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "auth_type")
@Table(name = "suseSCCRepositoryAuth")
@NamedNativeQuery(name = "SCCRepositoryAuth.lookupRepoIdWithAuth",
        query = "select distinct ra.repo_id from suseSCCRepositoryAuth ra")
public abstract class SCCRepositoryAuth extends BaseDomainHelper {

    private Long id;
    private SCCRepository repo;
    private Credentials credentials;
    private ContentSource contentSource;

    /**
     * Gets the id.
     * @return the id
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sccrepository_seq")
    @SequenceGenerator(name = "sccrepository_seq", sequenceName = "suse_sccrepository_id_seq",
                       allocationSize = 1)
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     * @param idIn the new id
     */
    public void setId(Long idIn) {
        id = idIn;
    }

    /**
     * Get the mirror credentials.
     * @return the credentials or null in case of fromdir
     */
    @ManyToOne(targetEntity = BaseCredentials.class)
    @JoinColumn(name = "credentials_id", nullable = true)
    protected Credentials getCredentials() {
        return credentials;
    }

    /**
     *  Get the mirror credentials. In case of fromdir is used
     *  an Optional.empty() is returned
     * @return the credentials or empty
     */
    @Transient
    public Optional<RemoteCredentials> getOptionalCredentials() {
        return Optional.ofNullable(credentials).flatMap(c -> c.castAs(RemoteCredentials.class));
    }

    /**
     * Set the mirror credentials this repo can be retrieved with.
     * @param credentialsIn the credentials to set
     */
    public void setCredentials(RemoteCredentials credentialsIn) {
        credentials = credentialsIn;
    }

    /**
     * @return the contentSource
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    public ContentSource getContentSource() {
        return contentSource;
    }

    /**
     * @param contentSourceIn the contentSource to set
     */
    public void setContentSource(ContentSource contentSourceIn) {
        contentSource = contentSourceIn;
    }

    /**
     * @return Returns the products.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    public SCCRepository getRepo() {
        return repo;
    }

    /**
     * @param repoIn the repository to set
     */
    public void setRepo(SCCRepository repoIn) {
        repo = repoIn;
    }

    /**
     * return url with authentication parameters
     * @return the url
     */
    @Transient
    public abstract String getUrl();

    /**
     * Maps this value of SCCRepositoryAuth to a value of type T. Depending on
     * What variant of SCCRepositoryAuth this is it will use the appropriate
     * supplied mapping function.
     *
     * @param basicAuth function for mapping basic auth
     * @param noAuth function for mapping no auth
     * @param tokenAuth function for mapping token auth
     * @param cloudRmtAuth function for mapping cloud rmt auth
     * @param <T> result value type
     * @return a value of type T returned by one of the mapping functions.
     */
    public abstract  <T> T fold(Function<SCCRepositoryBasicAuth, ? extends T> basicAuth,
               Function<SCCRepositoryNoAuth, ? extends T> noAuth,
               Function<SCCRepositoryTokenAuth, ? extends T> tokenAuth,
               Function<SCCRepositoryCloudRmtAuth, ? extends T> cloudRmtAuth);

    /**
     * @return {@link Optional} {@link SCCRepositoryBasicAuth}
     */
    public Optional<SCCRepositoryBasicAuth> basicAuth() {
        return fold(Optional::of, n -> Optional.empty(), t -> Optional.empty(), c -> Optional.empty());
    }

    /**
     * @return {@link Optional} {@link SCCRepositoryTokenAuth}
     */
    public Optional<SCCRepositoryTokenAuth> tokenAuth() {
        return fold(b -> Optional.empty(), n -> Optional.empty(), Optional::of, c -> Optional.empty());
    }

    /**
     * @return {@link Optional} {@link SCCRepositoryNoAuth}
     */
    public Optional<SCCRepositoryNoAuth> noAuth() {
        return fold(b -> Optional.empty(), Optional::of, t -> Optional.empty(), c -> Optional.empty());
    }

    /**
     * @return {@link Optional} {@link SCCRepositoryCloudRmtAuth}
     */
    public Optional<SCCRepositoryCloudRmtAuth> cloudRmtAuth() {
        return fold(b -> Optional.empty(), n -> Optional.empty(), t -> Optional.empty(), Optional::of);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SCCRepositoryAuth otherSCCRepository)) {
            return false;
        }
        return new EqualsBuilder()
            .append(getCredentials(), otherSCCRepository.getCredentials())
            .append(getRepo(), otherSCCRepository.getRepo())
            .append(getUrl(), otherSCCRepository.getUrl())
            .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getCredentials())
            .append(getRepo())
            .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", getId())
                .append("repo", Objects.toString(getRepo()))
                .toString();
    }
}
