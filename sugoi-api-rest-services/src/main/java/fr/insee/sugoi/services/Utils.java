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
package fr.insee.sugoi.services;

import fr.insee.sugoi.core.model.ProviderResponse;
import org.springframework.http.HttpStatus;

public class Utils {

  public static HttpStatus convertStatusTHttpStatus(
      ProviderResponse response, Boolean isCreation, Boolean noBody) {
    switch (response.getStatus()) {
      case PENDING:
        return HttpStatus.PROCESSING;
      case ACCEPTED:
        return HttpStatus.ACCEPTED;
      case OK:
        if (isCreation) {
          return HttpStatus.CREATED;
        }
        if (noBody) {
          return HttpStatus.NO_CONTENT;
        }
        return HttpStatus.OK;
      case KO:
        return HttpStatus.INTERNAL_SERVER_ERROR;
      case REQUESTED:
        return HttpStatus.ACCEPTED;
      default:
        throw new RuntimeException("unknown provider status");
    }
  }
}
