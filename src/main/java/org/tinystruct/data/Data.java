/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Condition;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;

public interface Data {

    String getClassPath();

    String getClassName();

    void setTableName(String attribute);

    Object setId(Object Id);

    Object getId();

    boolean append() throws ApplicationException;

    boolean update() throws ApplicationException;

    boolean delete() throws ApplicationException;

    Data setRequestFields(String fields);

    Data orderBy(String[] fieldNames);

    Table find(String SQL, Object[] parameters) throws ApplicationException;

    Table find(Condition condition, Object[] parameters) throws ApplicationException;

    Table findWith(String where, Object[] parameters) throws ApplicationException;

    Row findOne(String SQL, Object[] parameters) throws ApplicationException;

    Row findOneById() throws ApplicationException;

    Row findOneByKey(String PK, String value) throws ApplicationException;

    Table findAll() throws ApplicationException;

    Repository getRepository();

}
