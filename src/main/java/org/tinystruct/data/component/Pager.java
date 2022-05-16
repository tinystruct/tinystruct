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
    private String firstPageText, lastPageText, currentPageText, nextPageText,
            endPageText, controlBarText;
    /**
     * List Size
     */
    private int size;

    /**
     * How many pages for the list, auto calculate
     */
    private int pagesCount;
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
            this.pagesCount = (this.size - (this.size % this.pageSize))
                    / this.pageSize;
        } else {
            this.pagesCount = (this.size - (this.size % this.pageSize))
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
        StringBuilder pageControlBar = new StringBuilder();
        for (int i = 1; i <= pagesCount; i++) {
            pageControlBar.append(" <a href=\"").append(page).append("=").append(i).append("\">").append(i).append("</a> ");
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
        String theFirstPageLink = page + "=1", theLastPageLink = page
                + "=" + (currentPage - 1), theCurrentPageLink = page + "="
                + currentPage, theNextPageLink = page + "="
                + (currentPage + 1), theEndPageLink = page + "="
                + pagesCount, theFirstPage = (currentPage > 1) ? "<a href=\""
                + theFirstPageLink + "\">" + this.firstPageText + "</a>"
                : this.firstPageText, theLastPage = (currentPage > 1) ? "<a href=\""
                + theLastPageLink + "\">" + this.lastPageText + "</a>"
                : this.lastPageText,

                theCurrentPageText = "[<a href=\"" + theCurrentPageLink + "\">"
                        + currentPage + "</a>]", theCurrentPage = String.format(
                this.currentPageText, theCurrentPageText), theNextPage = (currentPage < pagesCount) ? "<a href=\""
                + theNextPageLink + "\">" + this.nextPageText + "</a>"
                : this.nextPageText, theEndPage = (currentPage < pagesCount) ? "<a href=\""
                + theEndPageLink + "\">" + this.endPageText + "</a>"
                : this.endPageText;

        Object[] args = new Object[]{pagesCount, pageSize, size,
                theFirstPage, theLastPage, theCurrentPage, theNextPage,
                theEndPage};

        return "<div class=\"pageControl\">"
                + String.format(this.controlBarText, args) + "</div>";
    }

}