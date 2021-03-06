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

public enum SendMode {
  MAIL("mail"),
  LETTER("letter");

  private String sendMode;

  private SendMode(String sendMode) {
    this.setSendMode(sendMode);
  }

  public void setSendMode(String sendMode) {
    this.sendMode = sendMode;
  }

  public String getSendMode() {
    return sendMode;
  }
}
