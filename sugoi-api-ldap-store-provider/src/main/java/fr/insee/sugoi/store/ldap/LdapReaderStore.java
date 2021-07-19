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
package fr.insee.sugoi.store.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import fr.insee.sugoi.core.store.ReaderStore;
import fr.insee.sugoi.ldap.utils.LdapFactory;
import fr.insee.sugoi.ldap.utils.LdapFilter;
import fr.insee.sugoi.ldap.utils.LdapUtils;
import fr.insee.sugoi.ldap.utils.config.LdapConfigKeys;
import fr.insee.sugoi.ldap.utils.mapper.AddressLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.ApplicationLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.GroupLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.LdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.OrganizationLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.UserLdapMapper;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LdapReaderStore extends LdapStore implements ReaderStore {

  public LdapReaderStore(Map<String, String> config, Map<String, Map<String, String>> mappings) {
    logger.debug("Configuring LdapReaderStore with config : {}", config);
    try {
      this.ldapPoolConnection = LdapFactory.getConnectionPool(config);
      this.ldapMonoConnection = LdapFactory.getSingleConnection(config);
      this.config = config;
      userLdapMapper = new UserLdapMapper(config, mappings.get("userMapping"));
      organizationLdapMapper =
          new OrganizationLdapMapper(config, mappings.get("organizationMapping"));
      groupLdapMapper = new GroupLdapMapper(config, mappings.get("groupMapping"));
      applicationLdapMapper = new ApplicationLdapMapper(config, mappings.get("applicationMapping"));
      addressLdapMapper = new AddressLdapMapper(config);
    } catch (LDAPException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieve the user ldap resource then complete it by retrieving the address ldap resource and
   * the organization ldap resource
   */
  @Override
  public User getUser(String id) {
    logger.debug("Searching user {}", id);
    SearchResultEntry entry = getEntryByDn(getUserDN(id));
    User user = (entry != null) ? userLdapMapper.mapFromAttributes(entry.getAttributes()) : null;
    if (user != null && user.getAddress() != null && user.getAddress().containsKey("id")) {
      Map<String, String> address = getAddress(user.getAddress().get("id"));
      if (address != null) {
        address.put("id", user.getAddress().get("id"));
        user.setAddress(address);
      }
    }
    if (user != null && user.getOrganization() != null) {
      user.setOrganization(getOrganization(user.getOrganization().getIdentifiant()));
    }
    return user;
  }

  /**
   * Retrieve the organization ldap resource then complete it by retrieving the address ldap
   * resource and the sub organization ldap resource
   */
  @Override
  public Organization getOrganization(String id) {
    if (config.get(LdapConfigKeys.ORGANIZATION_SOURCE) != null) {
      SearchResultEntry entry = getEntryByDn(getOrganizationDN(id));
      Organization org =
          (entry != null) ? organizationLdapMapper.mapFromAttributes(entry.getAttributes()) : null;
      if (org != null && org.getAddress() != null && org.getAddress().containsKey("id")) {
        Map<String, String> address = getAddress(org.getAddress().get("id"));
        if (address != null) {
          address.put("id", org.getAddress().get("id"));
          org.setAddress(address);
        }
      }
      if (org != null && org.getOrganization() != null) {
        org.setOrganization(getOrganization(org.getOrganization().getIdentifiant()));
      }
      return org;
    } else {
      throw new UnsupportedOperationException(
          "Organizations feature not configured for this storage");
    }
  }

  /** Search users matching userFilter set properties under the user_source */
  @Override
  public PageResult<User> searchUsers(
      User userFilter, PageableResult pageable, String typeRecherche) {
    try {
      return searchOnLdap(
          config.get(LdapConfigKeys.USER_SOURCE),
          SearchScope.SUBORDINATE_SUBTREE,
          getFilterFromObject(userFilter, userLdapMapper, typeRecherche),
          pageable,
          userLdapMapper);
    } catch (LDAPSearchException e) {
      throw new RuntimeException("Fail to execute user search", e);
    }
  }

  @Override
  public PageResult<User> getUsersInGroup(String appName, String groupName) {
    PageResult<User> page = new PageResult<>();
    SearchResultEntry entry = getEntryByDn(getGroupDN(appName, groupName));
    if (entry != null && entry.hasAttribute("uniqueMember")) {
      page.setResults(
          Arrays.stream(entry.getAttribute("uniqueMember").getValues())
              .map(uniqueMember -> getUser(LdapUtils.getNodeValueFromDN(uniqueMember)))
              .filter(user -> user != null)
              .collect(Collectors.toList()));
    } else {
      page.setResults(new ArrayList<>());
    }
    return page;
  }

  @Override
  public PageResult<Organization> searchOrganizations(
      Organization organizationFilter, PageableResult pageable, String searchOperator) {
    if (config.get(LdapConfigKeys.ORGANIZATION_SOURCE) != null) {
      try {
        return searchOnLdap(
            config.get(LdapConfigKeys.ORGANIZATION_SOURCE),
            SearchScope.SUBORDINATE_SUBTREE,
            getFilterFromObject(organizationFilter, organizationLdapMapper, searchOperator),
            pageable,
            organizationLdapMapper);
      } catch (LDAPSearchException e) {
        throw new RuntimeException("Fail to search organizations in ldap", e);
      }
    } else {
      throw new UnsupportedOperationException(
          "Organizations feature not configured for this storage");
    }
  }

  /**
   * Retrieve the specified group with the members' username. A check is made ex post to verify that
   * the retrieve object is a group as defined in the group_filter_pattern
   */
  @Override
  public Group getGroup(String appName, String groupName) {
    try {
      SearchResultEntry entry = getEntryByDn(getGroupDN(appName, groupName));
      return ((entry != null)
              && (Filter.create(getGroupWildcardFilter(appName)).matchesEntry(entry)))
          ? groupLdapMapper.mapFromAttributes(entry.getAttributes())
          : null;
    } catch (LDAPException e) {
      throw new RuntimeException("Fail to get group in ldap", e);
    }
  }

  /** Search groups with the group_filter under the group source */
  @Override
  public PageResult<Group> searchGroups(
      String appName, Group groupFilter, PageableResult pageable, String searchOperator) {
    try {
      return searchOnLdap(
          getGroupSource(appName),
          SearchScope.SUBORDINATE_SUBTREE,
          Filter.createANDFilter(
              Filter.create(getGroupWildcardFilter(appName)),
              getFilterFromObject(groupFilter, groupLdapMapper, searchOperator)),
          pageable,
          groupLdapMapper);
    } catch (LDAPException e) {
      throw new RuntimeException("Fail to search groups in ldap", e);
    }
  }

  @Override
  public boolean validateCredentials(User user, String credential) {
    try {
      return LdapFactory.validateUserPassword(config, getUserDN(user.getUsername()), credential);
    } catch (LDAPException e) {
      return false;
    }
  }

  @Override
  public Application getApplication(String applicationName) {
    SearchResultEntry entry = getEntryByDn(getApplicationDN(applicationName));
    Application application =
        (entry != null) ? applicationLdapMapper.mapFromAttributes(entry.getAttributes()) : null;
    if (application != null) {
      List<Group> groups = new ArrayList<>();
      try {
        groups =
            searchGroups(applicationName, new Group(), new PageableResult(), "AND").getResults();
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
      application.setGroups(groups);
    }
    return application;
  }

  /** Search applications matching applicationFilter set properties just under application source */
  @Override
  public PageResult<Application> searchApplications(
      Application applicationFilter, PageableResult pageable, String searchOperator) {
    try {
      if (config.get(LdapConfigKeys.APP_SOURCE) != null) {
        return searchOnLdap(
            config.get(LdapConfigKeys.APP_SOURCE),
            SearchScope.ONE,
            getFilterFromObject(applicationFilter, applicationLdapMapper, searchOperator),
            pageable,
            applicationLdapMapper);
      } else {
        throw new UnsupportedOperationException(
            "Applications feature not configured for this realm");
      }
    } catch (LDAPSearchException e) {
      throw new RuntimeException("Fail to search applications in ldap", e);
    }
  }

  /**
   * Create a filter from an object using a mapper class. Each set field of the object is
   * transformed to a filter.
   *
   * @param <MapperType> the type of object we need to create a filter from
   * @param object the object to create a filter from, only set properties are taken into account
   * @param mapper a mapper used to transform object to filter
   * @return a filter corresponding to the properties of object
   */
  private <MapperType> Filter getFilterFromObject(
      MapperType object, LdapMapper<MapperType> mapper, String searchType) {
    if (searchType.equalsIgnoreCase("AND")) {
      return LdapFilter.and(
          mapper.mapToAttributes(object).stream()
              .filter(attribute -> !attribute.getValue().equals(""))
              .map(attribute -> LdapFilter.contains(attribute.getName(), attribute.getValue()))
              .collect(Collectors.toList()));
    } else if (searchType.equalsIgnoreCase("OR")) {
      List<Filter> objectClassListFilter =
          mapper.mapToAttributes(object).stream()
              .filter(attribute -> attribute.getName().equals("objectClass"))
              .map(attribute -> LdapFilter.contains(attribute.getName(), attribute.getValue()))
              .collect(Collectors.toList());

      List<Filter> attributeListFilter =
          mapper.mapToAttributes(object).stream()
              .filter(
                  attribute ->
                      !attribute.getName().equals("objectClass")
                          && !attribute.getValue().equals(""))
              .map(attribute -> LdapFilter.contains(attribute.getName(), attribute.getValue()))
              .collect(Collectors.toList());

      if (objectClassListFilter.size() > 0 & attributeListFilter.size() == 0) {
        return LdapFilter.and(objectClassListFilter);
      } else if (objectClassListFilter.size() == 0 & attributeListFilter.size() > 0) {
        return LdapFilter.or(attributeListFilter);
      } else {
        return LdapFilter.and(
            Arrays.asList(
                LdapFilter.and(objectClassListFilter), LdapFilter.or(attributeListFilter)));
      }
    }
    throw new RuntimeException("Invalid searchType must be AND or OR");
  }

  private SearchResultEntry getEntryByDn(String dn) {
    try {
      logger.debug("Fetching {}", dn);
      SearchResultEntry entry = ldapPoolConnection.getEntry(dn, "+", "*");

      return entry;
    } catch (LDAPException e) {
      throw new RuntimeException("Failed to execute " + dn, e);
    }
  }

  /**
   * @param <ResultType> the type of the resource searched
   * @param baseDn DN where to make the search
   * @param scope search scope value
   * @param filter filter to apply on the search
   * @param pageableResult
   * @param mapper mapper to convert the attributes found to a ResultType
   * @return
   * @throws LDAPSearchException
   */
  private <ResultType> PageResult<ResultType> searchOnLdap(
      String baseDn,
      SearchScope scope,
      Filter filter,
      PageableResult pageableResult,
      LdapMapper<ResultType> mapper)
      throws LDAPSearchException {
    SearchRequest searchRequest = new SearchRequest(baseDn, scope, filter, "*", "+");
    LdapUtils.setRequestControls(searchRequest, pageableResult, config);
    SearchResult searchResult = null;
    try {
      searchResult = ldapMonoConnection.search(searchRequest);
    } catch (LDAPException e) {
      if (e.getResultCode().intValue() == ResultCode.SERVER_DOWN_INT_VALUE) {
        try {
          ldapMonoConnection = LdapFactory.getSingleConnection(config, true);
        } catch (LDAPException e1) {
          throw new RuntimeException(e1);
        }
        searchResult = ldapMonoConnection.search(searchRequest);
      } else {
        throw new RuntimeException(e);
      }
    }
    PageResult<ResultType> pageResult = new PageResult<>();
    pageResult.setResults(
        searchResult.getSearchEntries().stream()
            .map(e -> mapper.mapFromAttributes(e.getAttributes()))
            .collect(Collectors.toList()));
    LdapUtils.setResponseControls(pageResult, searchResult);
    pageResult.setNextStart(pageableResult.getFirst() + pageResult.getPageSize());
    return pageResult;
  }

  private Map<String, String> getAddress(String addressId) {
    SearchResultEntry addressResult = getEntryByDn(getAddressDN(addressId));
    return addressResult != null ? addressLdapMapper.mapFromSearchEntry(addressResult) : null;
  }
}
