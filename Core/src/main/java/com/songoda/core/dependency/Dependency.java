package com.songoda.core.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Dependency {
    private final String repositoryUrl;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private boolean relocate;
    private final List<Relocation> relocations;

    /**
     * @param repositoryUrl The repository url to download the dependency from.
     * @param groupId       The groupId of the dependency.
     * @param artifactId    The artifactId of the dependency.
     * @param version       The version of the dependency.
     */
    public Dependency(String repositoryUrl, String groupId, String artifactId, String version) {
        this(repositoryUrl, groupId, artifactId, version, true);
    }

    /**
     * @param repositoryUrl The repository url to download the dependency from.
     * @param groupId       The groupId of the dependency.
     * @param artifactId    The artifactId of the dependency.
     * @param version       The version of the dependency.
     * @param baseRelocate  If the dependency should be relocated to com.songoda.third_party.
     */
    public Dependency(String repositoryUrl, String groupId, String artifactId, String version, boolean baseRelocate) {
        this(repositoryUrl, groupId, artifactId, version, baseRelocate, new Relocation[0]);
    }

    /**
     * @param repositoryUrl    The repository url to download the dependency from.
     * @param groupId          The groupId of the dependency.
     * @param artifactId       The artifactId of the dependency.
     * @param version          The version of the dependency.
     * @param baseRelocate     If the dependency should be relocated to com.songoda.third_party.
     * @param extraRelocations Extra relocations to apply to the dependency.
     */
    public Dependency(String repositoryUrl, String groupId, String artifactId, String version, boolean baseRelocate, Relocation... extraRelocations) {
        this.relocations = new ArrayList<>();
        this.repositoryUrl = repositoryUrl;
        this.groupId = groupId.replaceAll(";", ".");
        this.artifactId = artifactId;
        this.version = version;
        if (baseRelocate) {
            // Add base relocate
            this.relocations.add(new Relocation(groupId, "com.songoda.third_party." + groupId));
        }
        if (extraRelocations.length > 0) {
            this.relocations.addAll(Arrays.asList(extraRelocations));
        }
    }

    public String getRepositoryUrl() {
        return this.repositoryUrl;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getVersion() {
        return this.version;
    }

    public List<Relocation> getRelocations() {
        return this.relocations;
    }

    public boolean shouldRelocate() {
        return this.relocate || !this.relocations.isEmpty();
    }

    public String buildArtifactUrl() {
        return this.repositoryUrl + "/" +
                this.groupId.replace('.', '/') + "/" +
                this.artifactId + "/" +
                this.version + "/" +
                this.artifactId + "-" + this.version + ".jar";
    }
}
