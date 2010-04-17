/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.spi;

/**
 * Query callback implementation which supports indexing.
 *
 */
public abstract class IndexedQueryCallback implements QueryCallback {
    protected int rowNum;

    /**
     * Called for each row in a result set.
     *
     * @param parameters parameters to get column values and other properties.
     */
    public void processRow(final ParametersCallback parameters) {
        processRow(parameters, rowNum);
        rowNum++;
    }

    /**
     * Called for each processed row.
     * @param parameters parameters to lookup variables.
     * @param rowNumber row number starting at 0.
     */
    protected abstract void processRow(final ParametersCallback parameters, final int rowNumber);


    /**
     * Returns number of processed rows.
     * @return number of processed rows.
     */
    public int getRowsNumber() {
        return rowNum;
    }
}
