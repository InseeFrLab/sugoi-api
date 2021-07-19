Feature: User scenario
    Performing actions on user

    Background: Use tomcat2
        Given the client is using tomcat1
        Given the client authentified with username appli_sugoi and password sugoi

    Scenario: Get users
        When the client perform GET request on url /realms/domaine1/users
        Then the client receives status code 200
        Then the client expect to receive a list of users
        Then the client want to see the users list

    Scenario: Post users
        When the client perform POST request with body on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users body:
            """
            {
                "lastName": "string",
                "firstName": "string",
                "mail": "string",
                "username": "string2"
            }
            """
        Then the client receives status code 201
        When the client perform GET request on url /realms/domaine1/users
        Then the client receives status code 200
        Then the client expect to receive a list of users
        Then the client want to see the users list

