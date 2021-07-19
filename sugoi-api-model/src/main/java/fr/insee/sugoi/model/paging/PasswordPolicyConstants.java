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

public class PasswordPolicyConstants implements Serializable {

  public static final String CREATE_PASSWORD_WITH_UPPERCASE = "create_password_WITHUpperCase";
  public static final String CREATE_PASSWORD_WITH_LOWERCASE = "create_password_WITHLowerCase";
  public static final String CREATE_PASSWORD_WITH_DIGITS = "create_password_WITHDigit";
  public static final String CREATE_PASSWORD_WITH_SPECIAL = "create_password_WITHSpecial";
  public static final String CREATE_PASSWORD_SIZE = "create_password_size";

  public static final String VALIDATE_PASSWORD_WITH_UPPERCASE = "validate_password_WITHUpperCase";
  public static final String VALIDATE_PASSWORD_WITH_LOWERCASE = "validate_password_WITHLowerCase";
  public static final String VALIDATE_PASSWORD_WITH_DIGITS = "validate_password_WITHDigit";
  public static final String VALIDATE_PASSWORD_WITH_SPECIAL = "validate_password_WITHSpecial";
  public static final String VALIDATE_PASSWORD_MIN_SIZE = "validate_password_size";
}
