package com.sightd.jenkins.gitlab;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.util.Collections;
import java.util.List;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.Test;

public class MergeRequestLabelFilterTraitTest {

    @Test
    public void includesMergeRequestWithConfiguredLabel() {
        assertFalse(filterFor("Ready For Tests").isExcluded(requestWithLabel("Ready For Tests"), headFor(7)));
    }

    @Test
    public void excludesMergeRequestWithoutConfiguredLabel() {
        assertTrue(filterFor("Ready For Tests").isExcluded(requestWithLabel("In Progress"), headFor(7)));
    }

    @Test
    public void excludesMergeRequestWithoutLabels() {
        MergeRequest mergeRequest = mock(MergeRequest.class);
        when(mergeRequest.getIid()).thenReturn(7L);
        when(mergeRequest.getLabels()).thenReturn(Collections.emptyList());

        assertTrue(filterFor("Ready For Tests").isExcluded(requestWith(mergeRequest), headFor(7)));
    }

    private MergeRequestLabelFilterTrait.MergeRequestLabelFilter filterFor(String label) {
        return new MergeRequestLabelFilterTrait.MergeRequestLabelFilter(label);
    }

    private GitLabSCMSourceRequest requestWithLabel(String title) {
        MergeRequest mergeRequest = mock(MergeRequest.class);
        when(mergeRequest.getIid()).thenReturn(7L);
        when(mergeRequest.getLabels()).thenReturn(List.of(title));
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
