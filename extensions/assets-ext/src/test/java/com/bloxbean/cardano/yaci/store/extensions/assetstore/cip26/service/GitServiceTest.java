package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingUpdateDetails;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("GitService")
class GitServiceTest {

    private GitService gitService;

    @TempDir
    Path tempDir;

    private Git testRepo;

    @BeforeEach
    void setUp() {
        Cip26NetworkDefaults networkDefaults = mock(Cip26NetworkDefaults.class);
        AssetsExtStoreProperties props = new AssetsExtStoreProperties();
        gitService = new GitService(networkDefaults, props);
        gitService.organization = "test-org";
        gitService.projectName = "test-repo";
        gitService.mappingsFolderName = "mappings";
        gitService.gitTempFolder = tempDir.toString();
        gitService.forceClone = false;
    }

    @AfterEach
    void tearDown() {
        gitService.cleanup();
        if (testRepo != null && testRepo != gitService.git) {
            testRepo.close();
        }
        testRepo = null;
    }

    private Git initRepoWithMappings() throws GitAPIException, IOException {
        Path repoDir = tempDir.resolve("test-repo");
        Files.createDirectories(repoDir.resolve("mappings"));
        Git git = Git.init().setDirectory(repoDir.toFile()).call();

        Files.writeString(repoDir.resolve("README.md"), "init");
        git.add().addFilepattern("README.md").call();
        git.commit().setMessage("initial commit")
                .setAuthor(new PersonIdent("Init", "init@test.com"))
                .call();

        return git;
    }

    private RevCommit addMappingFile(Git git, String fileName, String content, String email) throws Exception {
        Path mappingsDir = git.getRepository().getWorkTree().toPath().resolve("mappings");
        Files.createDirectories(mappingsDir);
        Files.writeString(mappingsDir.resolve(fileName), content);
        git.add().addFilepattern("mappings/" + fileName).call();
        return git.commit().setMessage("add " + fileName)
                .setAuthor(new PersonIdent("Test Author", email))
                .call();
    }

    @Nested
    @DisplayName("getHeadCommitHash")
    class GetHeadCommitHash {

        @Test
        void returnsHashFromRepo() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            Optional<String> result = gitService.getHeadCommitHash();

            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(40).matches("[0-9a-f]+");
        }

        @Test
        void returnsEmptyWhenGitIsNull() {
            assertThat(gitService.getHeadCommitHash()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getChangedFiles")
    class GetChangedFiles {

        @Test
        void returnsChangedMappingFiles() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            String oldHash = testRepo.log().setMaxCount(1).call().iterator().next().getName();
            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            String newHash = testRepo.log().setMaxCount(1).call().iterator().next().getName();

            List<Path> changed = gitService.getChangedFiles(oldHash, newHash);

            assertThat(changed).hasSize(1);
            assertThat(changed.getFirst().getFileName().toString()).isEqualTo("token1.json");
        }

        @Test
        void returnsEmptyWhenGitIsNull() {
            assertThat(gitService.getChangedFiles("aaa", "bbb")).isEmpty();
        }

        @Test
        void returnsEmptyForUnresolvableHashes() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            assertThat(gitService.getChangedFiles("0000000000000000000000000000000000000000",
                    "1111111111111111111111111111111111111111")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllMappingDetails")
    class GetAllMappingDetails {

        @Test
        @DisplayName("resolves files from root commit (single-commit repo)")
        void resolvesFilesFromRootCommit() throws Exception {
            // This is the exact scenario that broke before the recursive TreeWalk fix:
            // a repo with only one commit containing mapping files.
            Path repoDir = tempDir.resolve("test-repo");
            Files.createDirectories(repoDir.resolve("mappings"));
            testRepo = Git.init().setDirectory(repoDir.toFile()).call();

            Files.writeString(repoDir.resolve("mappings/token1.json"), "{}");
            Files.writeString(repoDir.resolve("mappings/token2.json"), "{}");
            testRepo.add().addFilepattern("mappings/").call();
            testRepo.commit().setMessage("initial with mappings")
                    .setAuthor(new PersonIdent("Author", "author@test.com"))
                    .call();
            gitService.git = testRepo;

            Map<String, MappingUpdateDetails> result = gitService.getAllMappingDetails(
                    Set.of("token1.json", "token2.json"));

            assertThat(result).hasSize(2);
            assertThat(result).containsKeys("token1.json", "token2.json");
            assertThat(result.get("token1.json").updatedBy()).isEqualTo("author@test.com");
            assertThat(result.get("token2.json").updatedBy()).isEqualTo("author@test.com");
        }

        @Test
        void resolvesFilesAcrossMultipleCommits() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            addMappingFile(testRepo, "token1.json", "{}", "first@test.com");
            addMappingFile(testRepo, "token2.json", "{}", "second@test.com");

            Map<String, MappingUpdateDetails> result = gitService.getAllMappingDetails(
                    Set.of("token1.json", "token2.json"));

            assertThat(result).hasSize(2);
            assertThat(result.get("token1.json").updatedBy()).isEqualTo("first@test.com");
            assertThat(result.get("token2.json").updatedBy()).isEqualTo("second@test.com");
        }

        @Test
        void returnsLatestCommitForUpdatedFile() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            addMappingFile(testRepo, "token1.json", "{\"v\":1}", "old@test.com");
            addMappingFile(testRepo, "token1.json", "{\"v\":2}", "new@test.com");

            Map<String, MappingUpdateDetails> result = gitService.getAllMappingDetails(
                    Set.of("token1.json"));

            assertThat(result).hasSize(1);
            assertThat(result.get("token1.json").updatedBy()).isEqualTo("new@test.com");
        }

        @Test
        void returnsEmptyMapWhenGitIsNull() {
            Map<String, MappingUpdateDetails> result = gitService.getAllMappingDetails(
                    Set.of("token1.json"));

            assertThat(result).isEmpty();
        }

        @Test
        void ignoresUnrequestedFiles() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            addMappingFile(testRepo, "token2.json", "{}", "dev@test.com");

            Map<String, MappingUpdateDetails> result = gitService.getAllMappingDetails(
                    Set.of("token1.json"));

            assertThat(result).hasSize(1);
            assertThat(result).containsKey("token1.json");
            assertThat(result).doesNotContainKey("token2.json");
        }
    }

    @Nested
    @DisplayName("remote-URL mismatch detection")
    class RemoteUrlMismatch {

        /**
         * Initialise an on-disk repo at {@code <tempDir>/<projectName>} with a
         * {@code remote.origin.url} of our choosing — simulates a leftover
         * clone from a previous run pointing at some upstream.
         */
        private void writeExistingCloneWithRemote(String remoteUrl) throws Exception {
            Path repoDir = tempDir.resolve(gitService.projectName);
            Files.createDirectories(repoDir);
            Git initialized = Git.init().setDirectory(repoDir.toFile()).call();
            try {
                org.eclipse.jgit.lib.StoredConfig config = initialized.getRepository().getConfig();
                config.setString("remote", "origin", "url", remoteUrl);
                config.save();
            } finally {
                initialized.close();
            }
        }

        @Test
        void readExistingRemoteUrlReturnsNullWhenFolderMissing() {
            // gitService.projectName="test-repo" not yet on disk under tempDir.
            assertThat(gitService.readExistingRemoteUrl()).isNull();
        }

        @Test
        void readExistingRemoteUrlReadsTheConfiguredOriginUrl() throws Exception {
            writeExistingCloneWithRemote("https://github.com/somebody/something.git");

            assertThat(gitService.readExistingRemoteUrl())
                    .isEqualTo("https://github.com/somebody/something.git");
        }

        @Test
        void expectedRemoteUrlBuildsFromOrgAndProject() {
            // setUp() set organization=test-org, projectName=test-repo
            assertThat(gitService.expectedRemoteUrl())
                    .isEqualTo("https://github.com/test-org/test-repo.git");
        }

        @Test
        void reasonToFreshCloneIsForceCloneWhenFlagSet() {
            gitService.forceClone = true;
            assertThat(gitService.reasonToFreshClone()).isEqualTo("force-clone enabled");
        }

        @Test
        void reasonToFreshCloneFlagsNonGitFolder() throws Exception {
            // Make a folder that exists but isn't a git repo.
            Files.createDirectories(tempDir.resolve(gitService.projectName));

            assertThat(gitService.reasonToFreshClone())
                    .isEqualTo("folder exists but is not a git repository");
        }

        @Test
        void reasonToFreshCloneFlagsRemoteUrlMismatch() throws Exception {
            // The leftover-mainnet-clone-on-a-preprod-config scenario from QA.
            writeExistingCloneWithRemote(
                    "https://github.com/cardano-foundation/cardano-token-registry.git");

            String reason = gitService.reasonToFreshClone();

            assertThat(reason).startsWith("remote URL mismatch");
            assertThat(reason).contains(
                    "existing='https://github.com/cardano-foundation/cardano-token-registry.git'");
            assertThat(reason).contains(
                    "expected='https://github.com/test-org/test-repo.git'");
        }

        @Test
        void reasonToFreshCloneIsNullWhenRemoteMatches() throws Exception {
            writeExistingCloneWithRemote("https://github.com/test-org/test-repo.git");

            assertThat(gitService.reasonToFreshClone()).isNull();
        }

        @Test
        void reasonToFreshCloneFlagsMissingRemoteUrl() throws Exception {
            // Repo exists, .git/config has no `remote.origin.url` at all.
            Path repoDir = tempDir.resolve(gitService.projectName);
            Files.createDirectories(repoDir);
            Git.init().setDirectory(repoDir.toFile()).call().close();

            String reason = gitService.reasonToFreshClone();

            assertThat(reason).startsWith("remote URL mismatch");
            assertThat(reason).contains("existing='null'");
        }
    }
}
