/**
 * Copyright 2011 Niklas Schmidtmer
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
package de.kabambo.maven.lesscss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which executes the lesscss compiler.
 *
 * @goal optimize
 * @phase compile
 */
public class OptimizeMojo extends AbstractMojo {
    /**
     * Directory containing *.less files.
     *
     * @parameter
     * @required
     */
    private File inputDirectory;

    /**
     * File where the compiled and combined output is to be written to.
     *
     * @parameter
     * @required
     */
    private File outputFile;

    /**
     * Indicates whether the generated CSS file should be compressed.
     *
     * @parameter
     */
    private boolean compress;

    /**
     * Path to lessc executable.
     *
     * @parameter
     * @required
     */
    private File lessCompiler;

    /**
     * Suffix of lesscss files.
     */
    private static final String LESS_SUFFIX = ".less";

    /**
     * Executes the MoJo.
     *
     * @throws MojoExecutionException In case execution failed
     */
    public void execute() throws MojoExecutionException {
        if (!inputDirectory.exists()) {
            throw new MojoExecutionException(String.format(
                "Directory %s does not exist.", inputDirectory.getAbsolutePath(
                    )));
        }

        if (!inputDirectory.isDirectory()) {
            throw new MojoExecutionException(String.format(
                "The path %s is not a directory.", inputDirectory.
                    getAbsolutePath()));
        }

        File[] containedFiles = inputDirectory.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(LESS_SUFFIX);
            }
        });

        StringBuffer s = new StringBuffer();
        for (File file : containedFiles) {
            s.append(convertFile(file));
        }

        try (BufferedWriter out = new BufferedWriter(new FileWriter(outputFile))) {
            // Make sure target file exists before writing
            outputFile.createNewFile();
            out.write(s.toString());
            out.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write file.", e);
        }
    }

    /**
     * Reads a file from filesystem and compiles the CSS from it.
     *
     * @param file file to read
     * @return generated CSS
     * @throws MojoExecutionException 
     */
    private String convertFile(File file) throws MojoExecutionException {
        Process p = buildProcess(file);

        try {
            if (p.waitFor() != 0) {
                throw new MojoExecutionException(
                    "Process terminated unexpectedly.");
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException(
                "Failed to wait for process to terminate.", e);
        }

        StringBuffer css = new StringBuffer();
        try (BufferedReader stream = new BufferedReader(new InputStreamReader(
                p.getInputStream()))) {
            String currentLine;
            while ((currentLine = stream.readLine()) != null) {
                css.append(currentLine);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Stream exited unexpectedly.", e);
        }

        return css.toString();
    }

    /**
     * Builds a process which performs the compilation.
     *
     * @param file file to compile
     * @return compilation process
     * @throws MojoExecutionException
     */
    private Process buildProcess(final File file) throws
            MojoExecutionException {
        List<String> args = new LinkedList<>();

        args.add(lessCompiler.getPath());
        args.add(file.getPath());

        if (compress) {
            args.add("-x");
        }

        try {
            return new ProcessBuilder(args).start();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to start process.", e);
        }
    }
}

