package org.carlspring.strongbox.db.schema;

public interface Edges
{

    String ARTIFACT_HAS_ARTIFACT_COORDINATES = "ArtifactHasArtifactCoordinates";
    String ARTIFACT_HAS_TAGS = "ArtifactHasTags";
    String ARTIFACT_GROUP_HAS_ARTIFACTS = "ArtifactGroupHasArtifacts";
    String ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS = "ArtifactGroupHasTaggedArtifacts";
    
    String EXTENDS = "Extends";

}
