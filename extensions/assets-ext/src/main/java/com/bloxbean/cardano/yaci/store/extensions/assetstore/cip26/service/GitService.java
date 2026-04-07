package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingUpdateDetails;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitService {

    private static final String GIT_NOT_INITIALIZED = "Git repository not initialized";

    private final Cip26NetworkDefaults networkDefaults;
    private final AssetsExtStoreProperties assetsStoreProperties;

    String gitTempFolder;
    boolean forceClone;

    String organization;
    String projectName;
    String mappingsFolderName;

    Git git;

    @PostConstruct
    void init() {
        organization = networkDefaults.getOrganization();
        projectName = networkDefaults.getProjectName();
        mappingsFolderName = networkDefaults.getMappingsFolder();

        gitTempFolder = assetsStoreProperties.getCip26().getGitTmpFolder();
        forceClone = assetsStoreProperties.getCip26().isForceClone();

        if (gitTempFolder == null || gitTempFolder.isBlank()) {
            log.warn("store.assets.ext.cip26.git.tmp-folder is blank, defaulting to system temp directory");
            gitTempFolder = System.getProperty("java.io.tmpdir");
        }
    }

    @PreDestroy
    void cleanup() {
        if (git != null) {
            git.close();
            git = null;
        }
    }

    public Optional<Path> cloneCardanoTokenRegistryGitRepository() {
        File gitFolder = getGitFolder();

        boolean repoReady;
        if (gitFolder.exists() && (forceClone || !isGitRepo())) {
            log.info("exists and either force clone or not a git repo");
            cleanup();
            FileSystemUtils.deleteRecursively(gitFolder);
            repoReady = cloneRepo();
        } else if (gitFolder.exists() && isGitRepo()) {
            log.info("exists and is git repo");
            repoReady = openExistingRepo() && pullRebaseRepo();
        } else {
            repoReady = cloneRepo();
        }

        if (repoReady) {
            return Optional.of(getMappingsFolder());
        } else {
            return Optional.empty();
        }
    }

    private boolean openExistingRepo() {
        try {
            cleanup();
            git = Git.open(getGitFolder());
            return true;
        } catch (IOException e) {
            log.warn("Failed to open existing git repository", e);
            return false;
        }
    }

    private boolean cloneRepo() {
        try {
            String url = String.format("https://github.com/%s/%s.git", organization, projectName);
            git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(getGitFolder())
                    .call();
            return true;
        } catch (GitAPIException e) {
            log.warn("It was not possible to clone the {} project", projectName, e);
            return false;
        }
    }

    private boolean pullRebaseRepo() {
        try {
            List<RemoteConfig> remotes = git.remoteList().call();
            if (remotes.isEmpty()) {
                log.info("No remote configured, skipping pull");
                return true;
            }
            git.pull()
                    .setRebase(BranchConfig.BranchRebaseMode.REBASE)
                    .call();
            return true;
        } catch (GitAPIException e) {
            log.warn("it was not possible to update repo. cloning from scratch", e);
            return false;
        }
    }

    private boolean isGitRepo() {
        return getGitFolder().toPath().resolve(".git").toFile().exists();
    }

    private File getGitFolder() {
        return Path.of(gitTempFolder).resolve(projectName).toFile();
    }

    private Path getMappingsFolder() {
        return getGitFolder().toPath().resolve(mappingsFolderName);
    }

    /**
     * Batch-resolve mapping update details for all files in a single git log walk.
     * Instead of running {@code git log --path=<file>} per file (O(files * commits)),
     * this walks the commit history once and records the most recent non-merge commit
     * that touched each file under the mappings folder — O(commits * tree-diff).
     */
    public Map<String, MappingUpdateDetails> getAllMappingDetails(Set<String> fileNames) {
        Map<String, MappingUpdateDetails> result = new HashMap<>();
        if (git == null) {
            log.warn(GIT_NOT_INITIALIZED);
            return result;
        }

        Set<String> remaining = new HashSet<>(fileNames);
        int commitCount = 0;

        try {
            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository)) {
                ObjectId head = repository.resolve("HEAD");
                if (head == null) {
                    log.warn("Could not resolve HEAD for batch mapping details");
                    return result;
                }
                revWalk.markStart(revWalk.parseCommit(head));
                revWalk.setRevFilter(RevFilter.NO_MERGES);

                try (ObjectReader reader = repository.newObjectReader()) {
                    for (RevCommit commit : revWalk) {
                        if (remaining.isEmpty()) {
                            break;
                        }
                        commitCount++;
                        recordTouchedMappings(repository, reader, commit, remaining, result);

                        if (commitCount % 1000 == 0) {
                            log.info("Git history walk: {} commits scanned, {}/{} files resolved",
                                    commitCount, result.size(), fileNames.size());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to batch-resolve mapping details after {} commits: {}", commitCount, e.getMessage());
        }

        log.info("Git history walk complete: {} commits scanned, {}/{} files resolved ({} unresolved)",
                commitCount, result.size(), fileNames.size(), remaining.size());
        return result;
    }

    private void recordTouchedMappings(Repository repository, ObjectReader reader, RevCommit commit,
                                         Set<String> remaining, Map<String, MappingUpdateDetails> result)
            throws IOException {
        List<String> touchedFiles = getFilesTouchedByCommit(repository, reader, commit);
        String prefix = mappingsFolderName + "/";
        for (String touchedPath : touchedFiles) {
            if (!touchedPath.startsWith(prefix)) {
                continue;
            }
            String fileName = touchedPath.substring(prefix.length());
            if (remaining.remove(fileName)) {
                PersonIdent author = commit.getAuthorIdent();
                result.put(fileName, new MappingUpdateDetails(
                        author.getEmailAddress(),
                        LocalDateTime.ofInstant(author.getWhenAsInstant(), ZoneOffset.UTC)));
            }
        }
    }

    private List<String> getFilesTouchedByCommit(Repository repository, ObjectReader reader, RevCommit commit)
            throws IOException {
        if (commit.getParentCount() == 0) {
            // Root commit — use recursive TreeWalk to find all files under mappings folder
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(mappingsFolderName));
                List<String> files = new ArrayList<>();
                while (treeWalk.next()) {
                    files.add(treeWalk.getPathString());
                }
                return files;
            }
        }

        try (RevWalk parentWalk = new RevWalk(repository)) {
            RevCommit parent = parentWalk.parseCommit(commit.getParent(0).getId());
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, parent.getTree().getId());
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, commit.getTree().getId());

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .setPathFilter(PathFilter.create(mappingsFolderName))
                    .call();

            return diffs.stream()
                    .map(d -> d.getChangeType() == DiffEntry.ChangeType.DELETE
                            ? d.getOldPath() : d.getNewPath())
                    .toList();
        } catch (GitAPIException e) {
            log.warn("Failed to diff commit {}: {}", commit.getName(), e.getMessage());
            return List.of();
        }
    }

    public Optional<String> getHeadCommitHash() {
        if (git == null) {
            log.warn(GIT_NOT_INITIALIZED);
            return Optional.empty();
        }
        try {
            Repository repository = git.getRepository();
            ObjectId head = repository.resolve("HEAD");
            if (head != null) {
                return Optional.of(head.name());
            }
        } catch (IOException e) {
            log.warn("Failed to get HEAD commit hash", e);
        }
        return Optional.empty();
    }

    public List<Path> getChangedFiles(String fromHash, String toHash) {
        if (git == null) {
            log.warn(GIT_NOT_INITIALIZED);
            return List.of();
        }
        try {
            Repository repository = git.getRepository();

            ObjectId oldId = repository.resolve(fromHash);
            ObjectId newId = repository.resolve(toHash);

            if (oldId == null || newId == null) {
                log.warn("Could not resolve commit hashes: {} -> {}", fromHash, toHash);
                return List.of();
            }

            AbstractTreeIterator oldTree = prepareTreeParser(repository, oldId);
            AbstractTreeIterator newTree = prepareTreeParser(repository, newId);

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .setPathFilter(PathFilter.create(mappingsFolderName))
                    .call();

            Path repoRoot = getGitFolder().toPath();
            return diffs.stream()
                    .filter(d -> d.getChangeType() == DiffEntry.ChangeType.ADD
                            || d.getChangeType() == DiffEntry.ChangeType.MODIFY)
                    .map(DiffEntry::getNewPath)
                    .filter(path -> path.endsWith(".json"))
                    .map(repoRoot::resolve)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to get changed files between {} and {}", fromHash, toHash, e);
        }
        return List.of();
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, commit.getTree().getId());
            }
            return treeParser;
        }
    }

}
