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
package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;

public interface Repository {
    public enum Type {MySQL, SQLServer, SQLite}

    ;

    public Type getType();

    public boolean append(Field ready_fields, String table) throws ApplicationException;

    public boolean update(Field ready_fields, String table) throws ApplicationException;

    public boolean delete(Object Id, String table) throws ApplicationException;

    public Table find(String SQL, Object[] parameters) throws ApplicationException;

    public Row findOne(String SQL, Object[] parameters) throws ApplicationException;
}
