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
package fr.insee.sugoi.ldap.utils.mapper;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Modification;
import fr.insee.sugoi.ldap.utils.config.LdapConfigKeys;
import fr.insee.sugoi.model.User;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UserLdapMapper implements LdapMapper<User> {

  Map<String, String> config;
  List<String> objectClasses;
  Map<String, String> mapping;

  public UserLdapMapper(Map<String, String> config, Map<String, String> mapping) {
    this.config = config;
    if (config.get(LdapConfigKeys.USER_OBJECT_CLASSES) != null) {
      objectClasses = Arrays.asList(config.get(LdapConfigKeys.USER_OBJECT_CLASSES).split(","));
    }
    this.mapping = mapping;
  }

  @Override
  public User mapFromAttributes(Collection<Attribute> attributes) {
    return GenericLdapMapper.mapLdapAttributesToObject(attributes, User.class, config, mapping);
  }

  @Override
  public List<Attribute> mapToAttributes(User u) {
    return GenericLdapMapper.mapObjectToLdapAttributes(
        u, User.class, config, mapping, objectClasses);
  }

  @Override
  public List<Modification> createMods(User updatedUser) {
    return GenericLdapMapper.createMods(updatedUser, User.class, config, mapping);
  }
}
