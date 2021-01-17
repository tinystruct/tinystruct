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
package org.tinystruct.application;

import org.tinystruct.data.FileEntity;

import java.util.List;

public interface Context {
    void setAttribute(String name, Object value);

    void removeAttribute(String name);

    Object getAttribute(String name);

    String[] getAttributeNames();

    List<String> getParameterValues(String name);

    void setParameter(String name, List<String> value);

    String getParameter(String name);

    void resetParameters();

    void setFiles(List<FileEntity> list);

    List<FileEntity> getFiles();
}