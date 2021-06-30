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
package fr.insee.sugoi.ldap.utils.config;

import fr.insee.sugoi.core.configuration.GlobalKeysConfig;

public class LdapConfigKeys extends GlobalKeysConfig {

  public static final String NAME = "name";
  public static final String URL = "url";
  public static final String PORT = "port";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String POOL_SIZE = "pool_size";
  public static final String GROUP_SOURCE_PATTERN = "group_source_pattern";
  public static final String GROUP_FILTER_PATTERN = "group_filter_pattern";
  public static final String REALM_NAME = "realm_name";
  public static final String TYPE = "TYPE";
  public static final String VLV_ENABLED = "vlvEnabled";
  public static final String SORT_KEY = "sortKey";
  public static final String USER_OBJECT_CLASSES = "user_object_classes";
  public static final String ORGANIZATION_OBJECT_CLASSES = "organization_object_classes";
  public static final String GROUP_OBJECT_CLASSES = "group_object_classes";
  public static final String APPLICATION_OBJECT_CLASSES = "application_object_classes";
  public static final String ADDRESS_OBJECT_CLASSES = "address_object_classes";
  public static final String USERSTORAGE_NAME = "userstorage_name";
}
