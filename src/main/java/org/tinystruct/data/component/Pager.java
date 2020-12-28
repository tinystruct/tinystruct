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
package org.tinystruct.data.component;

public class Pager {
    private StringBuffer pageControlBar;
    private String firstPageText, lastPageText, currentPageText, nextPageText,
            endPageText, controlBarText;
    /**
     * List Size
     */
    private int size;

    /**
     * How many pages for the list, auto calculate
     */
    private int pages_count;
    private int pageSize;
    private int currentPage;
    private int startIndex;

    public Pager() {
    }

    public Pager(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @return the startIndex
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @param startIndex the startIndex to set
     */
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * @return the pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @param pageSize the pageSize to set
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setCurrentPage(int page) {

        this.currentPage = page;
    }

    public void setListSize(int size) {
        this.size = size;
        this.startIndex = (this.currentPage - 1) * this.pageSize + 1;

        int n = (this.size % this.pageSize == 0) ? 0 : 1;
        if (n == 0) {
            this.pages_count = (this.size - (this.size % this.pageSize))
                    / this.pageSize;
        } else {
            this.pages_count = (this.size - (this.size % this.pageSize))
                    / this.pageSize + n;
        }
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    public String getPageControlNumber(String page) {
        pageControlBar = new StringBuffer();
        for (int i = 1; i <= pages_count; i++) {
            pageControlBar.append(" <a href=\"" + page + "=" + i + "\">" + i
                    + "</a> ");
        }
        return pageControlBar.toString();
    }

    public void setFirstPageText(String text) {
        this.firstPageText = text;
    }

    public void setLastPageText(String text) {
        this.lastPageText = text;
    }

    public void setCurrentPageText(String text) {
        this.currentPageText = text;
    }

    public void setNextPageText(String text) {
        this.nextPageText = text;
    }

    public void setEndPageText(String text) {
        this.endPageText = text;
    }

    public void setControlBarText(String text) {
        this.controlBarText = text;
    }

    public String getPageControlBar(String page) {
        String The_first_page_link = page + "=1", The_last_page_link = page
                + "=" + (currentPage - 1), The_current_page_link = page + "="
                + currentPage, The_next_page_link = page + "="
                + (currentPage + 1), The_end_page_link = page + "="
                + pages_count, The_first_page = (currentPage > 1) ? "<a href=\""
                + The_first_page_link + "\">" + this.firstPageText + "</a>"
                : this.firstPageText, The_last_page = (currentPage > 1) ? "<a href=\""
                + The_last_page_link + "\">" + this.lastPageText + "</a>"
                : this.lastPageText,

                The_current_page_text = "[<a href=\"" + The_current_page_link + "\">"
                        + currentPage + "</a>]", The_current_page = String.format(
                this.currentPageText, new Object[]{The_current_page_text}), The_next_page = (currentPage < pages_count) ? "<a href=\""
                + The_next_page_link + "\">" + this.nextPageText + "</a>"
                : this.nextPageText, The_end_page = (currentPage < pages_count) ? "<a href=\""
                + The_end_page_link + "\">" + this.endPageText + "</a>"
                : this.endPageText;

        Object[] args = new Object[]{pages_count, pageSize, size,
                The_first_page, The_last_page, The_current_page, The_next_page,
                The_end_page};

        return "<div class=\"pageControl\">"
                + String.format(this.controlBarText, args) + "</div>";
    }

}