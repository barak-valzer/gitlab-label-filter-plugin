package com.sightd.jenkins.gitlab;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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
    private final String includeLabels;
    private final String excludeLabels;

    public MergeRequestLabelFilterTrait(String label) {
        this(label, null);
    }

    @DataBoundConstructor
    public MergeRequestLabelFilterTrait(String includeLabels, String excludeLabels) {
        this.label = null;
        this.includeLabels = Util.fixEmptyAndTrim(includeLabels);
        this.excludeLabels = Util.fixEmptyAndTrim(excludeLabels);
    }

    public String getLabel() {
        return label;
    }

    public String getIncludeLabels() {
        return includeLabels != null ? includeLabels : label;
    }

    public String getExcludeLabels() {
        return excludeLabels;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            String includes = getIncludeLabels();
            if (includes != null || excludeLabels != null) {
                ((GitLabSCMSourceContext) context).withFilter(
                        new MergeRequestLabelFilter(includes, excludeLabels));
            }
        }
    }

    static final class MergeRequestLabelFilter extends SCMHeadFilter {

        private final List<Pattern> includePatterns;
        private final List<Pattern> excludePatterns;

        MergeRequestLabelFilter(String includeLabels, String excludeLabels) {
            this.includePatterns = compilePatterns(includeLabels);
            this.excludePatterns = compilePatterns(excludeLabels);
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
                    if (labels == null) {
                        return !includePatterns.isEmpty();
                    }
                    if (labels.stream().anyMatch(this::isExcludedLabel)) {
                        return true;
                    }
                    return !includePatterns.isEmpty()
                            && labels.stream().noneMatch(this::matchesIncludePattern);
                }
            }
            return true;
        }

        private boolean isExcludedLabel(String label) {
            return excludePatterns.stream().anyMatch(pattern -> pattern.matcher(label).matches());
        }

        private boolean matchesIncludePattern(String label) {
            return includePatterns.stream().anyMatch(pattern -> pattern.matcher(label).matches());
        }

        private static List<Pattern> compilePatterns(String labels) {
            List<Pattern> patterns = new ArrayList<>();
            if (labels == null) {
                return patterns;
            }
            for (String label : labels.split("[,\\r\\n]+")) {
                String trimmed = label.trim();
                if (!trimmed.isEmpty()) {
                    patterns.add(Pattern.compile(toRegex(trimmed)));
                }
            }
            return patterns;
        }

        private static String toRegex(String glob) {
            StringBuilder regex = new StringBuilder("^");
            for (char character : glob.toCharArray()) {
                switch (character) {
                    case '*':
                        regex.append(".*");
                        break;
                    case '?':
                        regex.append('.');
                        break;
                    default:
                        if ("\\\\.^$|()[]{}+".indexOf(character) >= 0) {
                            regex.append('\\');
                        }
                        regex.append(character);
                }
            }
            return regex.append('$').toString();
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
