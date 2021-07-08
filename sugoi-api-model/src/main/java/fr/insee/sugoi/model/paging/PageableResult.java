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

public class PageableResult implements Serializable {
  private static final int TAILLE_RECHERCHE_DEFAUT = 20;

  private int size = TAILLE_RECHERCHE_DEFAUT;
  private String searchToken;
  private int offset = 1;

  public PageableResult() {}

  public PageableResult(int size, int offset, String searchToken) {
    this.size = size;
    this.offset = offset;
    this.searchToken = searchToken;
  }

  public int getSize() {
    return this.size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getFirst() {
    return this.offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public String getSearchToken() {
    return this.searchToken;
  }

  public void setSearchToken(String searchToken) {
    this.searchToken = searchToken;
  }
}
