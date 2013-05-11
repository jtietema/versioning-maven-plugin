package net.tietema.versioning;

/*
 * Copyright 2013 Jeroen Tietema
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.squareup.java.JavaWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.Locale;

/**
 * Goal which generates a Java file with version information of the current git repository
 */
@Mojo(name = "git-java", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GitJavaVersioning extends AbstractMojo {
    /**
     * The base directory of the project. Should only be changed if you have an exotic project layout.
     */
    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File outputDirectory;

    /**
     * Java package name.
     */
    @Parameter(required = true)
    private String packageName;

    /**
     * The classname of the output file
     */
    @Parameter(required = true)
    private String className;

    /**
     * Your sources directory. Defaults to maven default (src/main/java)
     */
    @Parameter(defaultValue = "src/main/java", required = true)
    private String sourcesDir;

    private Log log;

    public void execute() throws MojoExecutionException {
        File f = outputDirectory;

        log = getLog();

        log.info("Dir is: " + outputDirectory.toString());

        if (!f.exists()) {
            f.mkdirs();
        }

        File touch = new File(f, sourcesDir + "/" + packageName.replace(".", "/") + "/" + className + ".java");

        FileWriter w = null;
        try {
            w = new FileWriter(touch);

            w.write(getRevision(outputDirectory));
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating file " + touch, e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public String getRevision(File projectDir) throws MojoExecutionException {
        // XXX we use our own findGitDir because they JGit one doesn't find the git dir in a multi project build
        File gitDir = findGitDir(projectDir);
        String revision = "Unknown";
        if (gitDir == null) return revision;

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = null;
        try {
            repository = builder
                    .setGitDir(gitDir)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir(projectDir) // scan up the file system tree
                    .build();

            log.info("Git dir: " + gitDir.toString());
            RepositoryState state = repository.getRepositoryState();
            log.info(state.getDescription());
            String branch = repository.getBranch();
            log.info("Branch is: " + branch);
            Git git =  new Git(repository);
            String fullBranch = repository.getFullBranch();
            log.info("Full branch is: " + fullBranch);
            ObjectId id = repository.resolve(fullBranch);
            log.info("Branch " + repository.getBranch() + " points to " + id.name());

            Status status = git.status().call();
            boolean strictClean = status.isClean();
            // no untracked files
            boolean loseClean = status.getAdded().isEmpty()
                    && status.getChanged().isEmpty()
                    && status.getConflicting().isEmpty()
                    && status.getMissing().isEmpty()
                    && status.getModified().isEmpty()
                    && status.getRemoved().isEmpty();

                    StringWriter buffer = new StringWriter();
            JavaWriter writer = new JavaWriter(buffer);
            writer.emitPackage(packageName)
                    .beginType(packageName + "." + className, "class", Modifier.PUBLIC | Modifier.FINAL)
                    .emitField("String",  "BRANCH", Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC, String.format(Locale.US, "\"%s\"", branch))
                    .emitField("String",  "REVISION", Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC, String.format(Locale.US, "\"%s\"", id.name()))
                    .emitField("String",  "REVISION_SHORT", Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC, String.format(Locale.US, "\"%s\"", id.name().substring(0, 8)))
                    .emitJavadoc("Strict Clean means no changes, not even untracked files")
                    .emitField("boolean", "STRICT_CLEAN", Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC, (strictClean ? "true" : "false"))
                    .emitJavadoc("Lose Clean means no changes except untracked files.")
                    .emitField("boolean", "LOSE_CLEAN", Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC, (loseClean ? "true" : "false"))
                    .endType();
            revision = buffer.toString();

            return revision;
        } catch (IOException e) {
            log.error(e);
            throw new MojoExecutionException(e.getMessage());
        } catch (GitAPIException e) {
            log.error(e);
            throw new MojoExecutionException(e.getMessage());
        } finally {
            if (repository != null)
                repository.close();
        }
    }

    private File findGitDir(File dir) {
        File gitDir = new File(dir.getAbsoluteFile(), ".git");
        if (gitDir.exists()) return gitDir;
        if (dir.getParentFile() == null) return null;
        return findGitDir(dir.getParentFile());
    }
}
