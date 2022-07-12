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
package org.tinystruct.dom;

public enum XHTML {
    Transitional(0), Strict(1), Frameset(2), Latin(3), Symbols(4), Special(5);
    public final String PUBLIC;
    public final String SYSTEM;

    XHTML(int index) {
        switch (index) {
            case 0:
                this.PUBLIC = "-//W3C//DTD XHTML 1.0 Transitional//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
                break;

            case 1:
                this.PUBLIC = "-//W3C//DTD XHTML 1.0 Strict//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";
                break;

            case 2:
                this.PUBLIC = "-//W3C//DTD XHTML 1.0 Frameset//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd";
                break;

            case 3:
                this.PUBLIC = "-//W3C//ENTITIES Latin 1 for XHTML//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent";
                break;

            case 4:
                this.PUBLIC = "-//W3C//ENTITIES Symbols for XHTML//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent";
                break;

            case 5:
                this.PUBLIC = "-//W3C//ENTITIES Special for XHTML//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent";
                break;

            default:
                this.PUBLIC = "-//W3C//DTD XHTML 1.0 Transitional//EN";
                this.SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
                break;
        }
    }

}
