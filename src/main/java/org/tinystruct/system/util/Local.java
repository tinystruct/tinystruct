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

import java.io.File;

public class Local {
    private String path;

    public Local(String path) {
        this.path = path;
    }

    public String getPath() {
        this.path = this.path.replace('/', File.separatorChar);
        if (!this.path.endsWith(File.separator))
            this.path += File.separator;
        return this.path;
    }

    @Override
	public String toString() {
        return this.path;
    }
}