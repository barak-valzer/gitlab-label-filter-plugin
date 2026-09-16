package com.sightd.jenkins.gitlab;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.util.List;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.gitlab4j.api.models.MergeRequest;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class MergeRequestLabelFilterTrait extends SCMSourceTrait {

    private final String label;

    @DataBoundConstructor
    public MergeRequestLabelFilterTrait(String label) {
        this.label = Util.fixEmptyAndTrim(label);
    }

    public String getLabel() {
        return label;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext && label != null) {
            ((GitLabSCMSourceContext) context).withFilter(new MergeRequestLabelFilter(label));
        }
    }

    static final class MergeRequestLabelFilter extends SCMHeadFilter {

        private final String label;

        MergeRequestLabelFilter(String label) {
            this.label = label;
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
            if (!(head instanceof MergeRequestSCMHead) || !(request instanceof GitLabSCMSourceRequest)) {
                return false;
            }

            long iid = Long.parseLong(((MergeRequestSCMHead) head).getId());
            for (MergeRequest mergeRequest : ((GitLabSCMSourceRequest) request).getMergeRequests()) {
                if (mergeRequest.getIid() == iid) {
                    List<String> labels = mergeRequest.getLabels();
                    return labels == null || !labels.contains(label);
                }
            }
            return true;
        }
    }

    @Extension
    @Symbol("gitLabMergeRequestLabelFilter")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Filter merge requests by label";
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitLabSCMSource.class;
        }
    }
}
