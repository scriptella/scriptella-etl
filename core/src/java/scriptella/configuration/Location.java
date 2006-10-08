/*
 * Copyright 2006 The Scriptella Project Team.
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
package scriptella.configuration;


/**
 * Defines xml element location.
 * <p>TODO: add support for row/columns
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Location {
    private int row;
    private int column;
    private String xpath;

    public Location(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public Location(String xpath) {
        this.xpath = xpath;
    }

    public int getRow() {
        return row;
    }

    void setRow(final int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    void setColumn(final int column) {
        this.column = column;
    }

    public String getXpath() {
        return xpath;
    }

    public String toString() {
        return getXpath();
    }
}
