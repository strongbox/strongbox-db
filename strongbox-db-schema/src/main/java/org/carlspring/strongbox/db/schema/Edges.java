package org.carlspring.strongbox.db.schema;

public interface Edges
{

    String ARTIFACT_HAS_ARTIFACT_COORDINATES = "ArtifactHasArtifactCoordinates";
    String ARTIFACT_HAS_TAGS = "ArtifactHasTags";
    String ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES = "ArtifactCoordinatesInheritGenericArtifactCoordinates";
    String ARTIFACT_GROUP_HAS_ARTIFACTS = "ArtifactGroupHasArtifacts";
    String REMOTE_ARTIFACT_INHERIT_ARTIFACT = "RemoteArtifactInheritArtifact";

}
