/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fr.insee.sugoi.model.paging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PageResult<T> implements Serializable {
  private static final int TAILLE_RECHERCHE_DEFAUT = 20;

  private List<T> results = new ArrayList<>();
  private int totalElements;
  private int nextStart;
  private boolean hasMoreResult = false;
  private int pageSize = TAILLE_RECHERCHE_DEFAUT;
  private String searchToken;

  public PageResult() {}

  public List<T> getResults() {
    return this.results;
  }

  public void setResults(List<T> results) {
    this.results = results;
  }

  public int getTotalElements() {
    return this.totalElements;
  }

  public void setTotalElements(int totalElements) {
    this.totalElements = totalElements;
  }

  public int getNextStart() {
    return this.nextStart;
  }

  public void setNextStart(int nextStart) {
    this.nextStart = nextStart;
  }

  public boolean isHasMoreResult() {
    return this.hasMoreResult;
  }

  public void setHasMoreResult(boolean hasMoreResult) {
    this.hasMoreResult = hasMoreResult;
  }

  public int getPageSize() {
    return this.pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public String getSearchToken() {
    return this.searchToken;
  }

  public void setSearchToken(String searchToken) {
    this.searchToken = searchToken;
  }
}
