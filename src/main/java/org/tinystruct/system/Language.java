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
package org.tinystruct.system;

public enum Language {
    zh_CN, zh_GB, zh_TW, en_GB, en_US, en_CA, fr_FR, fr_CA, de_DE, it_IT, ja_JP, ko_KR;

    public static boolean support(String lang) {
        Language[] codes = Language.values();
        for (Language code : codes) {
            if (code.toString().equalsIgnoreCase(lang)) return true;
        }

        return false;
    }
}