package com.sightd.jenkins.gitlab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.util.Collections;
import java.util.List;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.jupiter.api.Test;

public class MergeRequestLabelFilterTraitTest {

    @Test
    public void includesMergeRequestWithConfiguredLabel() {
        assertFalse(filterFor("Ready For Tests", null)
                .isExcluded(requestWithLabels("Ready For Tests"), headFor(7)));
    }

    @Test
    public void excludesMergeRequestWithoutConfiguredLabel() {
        assertTrue(filterFor("Ready For Tests", null)
                .isExcluded(requestWithLabels("In Progress"), headFor(7)));
    }

    @Test
    public void includesMergeRequestMatchingAnyConfiguredLabel() {
        assertFalse(filterFor("Ready For Tests,Approved", null)
                .isExcluded(requestWithLabels("Approved"), headFor(7)));
    }

    @Test
    public void includesMergeRequestMatchingWildcardPattern() {
        assertFalse(filterFor("Ready *", null)
                .isExcluded(requestWithLabels("Ready For Tests"), headFor(7)));
    }

    @Test
    public void excludesMergeRequestMatchingExcludePattern() {
        assertTrue(filterFor("*", "Do Not Build")
                .isExcluded(requestWithLabels("Do Not Build"), headFor(7)));
    }

    @Test
    public void excludePatternTakesPrecedenceOverIncludePattern() {
        assertTrue(filterFor("Ready *", "Ready For Review")
                .isExcluded(requestWithLabels("Ready For Review"), headFor(7)));
    }

    @Test
    public void excludesMergeRequestWithoutLabelsWhenIncludeIsConfigured() {
        MergeRequest mergeRequest = mock(MergeRequest.class);
        when(mergeRequest.getIid()).thenReturn(7L);
        when(mergeRequest.getLabels()).thenReturn(null);

        assertTrue(filterFor("Ready For Tests", null)
                .isExcluded(requestWith(mergeRequest), headFor(7)));
    }

    @Test
    public void includesMergeRequestWithoutLabelsWhenOnlyExcludeIsConfigured() {
        MergeRequest mergeRequest = mock(MergeRequest.class);
        when(mergeRequest.getIid()).thenReturn(7L);
        when(mergeRequest.getLabels()).thenReturn(Collections.emptyList());

        assertFalse(filterFor(null, "Do Not Build")
                .isExcluded(requestWith(mergeRequest), headFor(7)));
    }

    private MergeRequestLabelFilterTrait.MergeRequestLabelFilter filterFor(
            String includeLabels, String excludeLabels) {
        return new MergeRequestLabelFilterTrait.MergeRequestLabelFilter(includeLabels, excludeLabels);
    }

    private GitLabSCMSourceRequest requestWithLabels(String... labels) {
        MergeRequest mergeRequest = mock(MergeRequest.class);
        when(mergeRequest.getIid()).thenReturn(7L);
        when(mergeRequest.getLabels()).thenReturn(List.of(labels));
        return requestWith(mergeRequest);
    }

    private GitLabSCMSourceRequest requestWith(MergeRequest mergeRequest) {
        GitLabSCMSourceRequest request = mock(GitLabSCMSourceRequest.class);
        when(request.getMergeRequests()).thenReturn(List.of(mergeRequest));
        return request;
    }

    private MergeRequestSCMHead headFor(long iid) {
        MergeRequestSCMHead head = mock(MergeRequestSCMHead.class);
        when(head.getId()).thenReturn(Long.toString(iid));
        return head;
    }
}
