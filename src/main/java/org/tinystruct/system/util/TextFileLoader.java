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

public class TextFileLoader {
    private File file;
    private InputStream inputStream;
    private String charsetName = "utf-8";

    public TextFileLoader() {
    }

    public TextFileLoader(File file) {
        this.file = file;
    }

    public TextFileLoader(String fileName) {
        this.file = new File(fileName);
    }

    public TextFileLoader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setCharset(String charsetName) {
        this.charsetName = charsetName;
    }

    public StringBuilder getContent() throws ApplicationException {
        StringBuilder content = new StringBuilder();
        try {
            if (this.file != null) {
                this.inputStream = new FileInputStream(this.file);
            }

            if (this.inputStream == null) {
                throw new ApplicationException("The file input stream is not ready.");
            }

            InputStreamReader reader = new InputStreamReader(this.inputStream, this.charsetName);
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line = bufferedReader.readLine();
            while (line != null) {
                content.append(line).append("\r\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            reader.close();

            this.inputStream.close();
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage() + " - " + this.file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return content;
    }

    public String getFilePath() {
        return this.file.getPath();
    }

}