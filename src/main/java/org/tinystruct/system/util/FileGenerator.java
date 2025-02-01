/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.system.util;


import org.tinystruct.ApplicationException;

import java.io.*;
import java.util.logging.Logger;

public class FileGenerator {
    private final static Logger logger = Logger.getLogger(FileGenerator.class.getName());
    private final String filename;
    private StringBuilder content;
    private File file;

    public FileGenerator(String filename) {
        this.filename = filename;
    }

    public FileGenerator(String path, String filename, StringBuilder content) throws ApplicationException {
        boolean condition = !new File(path).isDirectory() && !new File(path.toLowerCase()).mkdir();
		if (condition)
			throw new ApplicationException("Make directory for preparing generate class file directory error");
        this.filename = path + File.separator + filename;
        this.file = new File(this.filename.toLowerCase());
        this.content = content;
    }

    public FileGenerator(String filename, StringBuilder content) throws ApplicationException {
        this.filename = filename;
        this.content = content;
        this.file = new File(this.filename);
    }

    /**
     * Save the content into the file that its name is 'filename'.
     */
    public void save() {
        try (FileOutputStream fos = new FileOutputStream(file); PrintWriter printer = new PrintWriter(fos, true)) {
            printer.write(this.content.toString());
        } catch (IOException io) {
            logger.severe("File cannot be created!Exception was happened:" + io.getMessage());
        } catch (NullPointerException NullPointer) {
            logger.severe("Please set the file,then use the operator!");
        }
    }

    /**
     * Get the File Content
     *
     * @return The content after read it.
     */
    public StringBuilder read() {
        this.content = new StringBuilder();
        try (FileInputStream in = new FileInputStream(new File(this.filename)); InputStreamReader reader = new InputStreamReader(in)) {
            int i = reader.read();
            while (i != -1) {
                this.content.append((char) i);
                i = reader.read();
            }
        } catch (IOException io) {
            logger.severe(io.getMessage());
        }

        return this.content;
    }
}