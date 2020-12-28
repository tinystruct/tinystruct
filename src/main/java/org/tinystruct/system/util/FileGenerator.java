/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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
    private final static Logger logger = Logger.getLogger("FileGenerator.class");
    ;
    private String filename;
    private StringBuffer content;
    private FileOutputStream fos;
    private File file;
    private PrintWriter printer;

    public FileGenerator(String filename) {
        this.filename = filename;
    }

    public FileGenerator(String path, String filename, StringBuffer content) throws ApplicationException {
        if (!new File(path).isDirectory()) if (!new File(path.toLowerCase()).mkdir())
            throw new ApplicationException("Make directory for preparing generate class file directory error");
        this.filename = path + File.separator + filename;
        this.file = new File(this.filename.toLowerCase());
        this.content = content;
    }

    public FileGenerator(String filename, StringBuffer content) throws ApplicationException {
        this.filename = filename;
        this.content = content;
        this.file = new File(this.filename);
    }

    /**
     * Save the content into the file that its name is 'filename'.
     */
    public void save() {
        try {
            fos = new FileOutputStream(file);
        } catch (NullPointerException NullPointer) {
            logger.severe("Please set the file,then use the operator!");
        } catch (FileNotFoundException FileNotFound) {
            logger.severe("FileOutputSteam cannot be created!Exception was happened:" + FileNotFound.getMessage());
        }

        try {
            this.printer = new PrintWriter(fos, true);
            this.printer.write(this.content.toString());
            this.printer.close();

            fos.close();
        } catch (IOException IO) {
            logger.severe("File cannot be created!Exception was happened:" + IO.getMessage());
        }
    }

    /**
     * Get the File Content
     *
     * @return The content after read it.
     */
    public StringBuffer read() {
        this.content = new StringBuffer();
        try {
            FileInputStream in = new FileInputStream(new File(this.filename));
            InputStreamReader reader = new InputStreamReader(in);
            int i = reader.read();
            while (i != -1) {
                this.content.append((char) i);
                i = reader.read();
            }
            reader.close();
            in.close();
        } catch (IOException io) {
            logger.severe(io.getMessage());
        }

        return this.content;
    }
}