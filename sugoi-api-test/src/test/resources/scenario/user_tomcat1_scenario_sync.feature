Feature: User scenario
    Performing actions on user

    Background: Use tomcat1
        Given the client is using tomcat1
        Given the client authentified with username appli_sugoi and password sugoi

    Scenario: Get users
        When the client perform GET request on url /realms/domaine1/users
        And show body received
        Then the client receives status code 200
        Then the client expect to receive a list of users

    Scenario: Get users with email filter
        When the client perform GET request on url /realms/domaine1/users?mail=test&size=20&offset=0&typeRecherche=AND
        And show body received
        Then the client receives status code 200
        Then The client expect to receive a list of 1 user(s)

    Scenario: Get user
        When the client perform GET request on url /realms/domaine1/users/testc
        And show body received
        Then the client receives status code 200
        Then the client expect to receive an user

    Scenario: Get user not exist
        When the client perform GET request on url /realms/domaine1/users/trtr
        And show body received
        Then the client receives status code 404

    Scenario: Post user
        When the client perform POST request with body on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users body:
            """
            {
                "username": "abcd"
            }
            """
        And show body received
        Then the client receives status code 201
        Then the client expect to receive an user

    Scenario: Post user already exist
        When the client perform POST request with body on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users body:
            """
            {
                "username": "abcd"
            }
            """
        And show body received
        Then the client receives status code 409

    Scenario: Update user
        When the client perform PUT request with body on url /realms/domaine1/users/abcd body:
            """
            {
                "username": "abcd",
                "mail": "abcd@test.fr"
            }
            """
        And show body received
        Then the client receives status code 200
        Then the client expect to receive an user


    Scenario: Update user not exist
        When the client perform PUT request with body on url /realms/domaine1/users/abcde body:
            """
            {
                "username": "abcde",
                "mail": "abcde@test.fr"
            }
            """
        And show body received
        Then the client receives status code 404

    Scenario: Delete user
        When the client perform DELETE request on url /realms/domaine1/users/abcd
        And show body received
        Then the client receives status code 204

    Scenario: Delete user not exist
        When the client perform DELETE request on url /realms/domaine1/users/abcde
        And show body received
        Then the client receives status code 404

    Scenario: Get users (storage)
        When the client perform GET request on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users
        And show body received
        Then the client receives status code 200
        Then the client expect to receive a list of users

    Scenario: Get user (storage)
        When the client perform GET request on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/testc
        And show body received
        Then the client receives status code 200
        Then the client expect to receive an user

    Scenario: Get user not exist (storage)
        When the client perform GET request on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/trtr
        And show body received
        Then the client receives status code 404

    Scenario: Post user
        When the client perform POST request with body on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users body:
            """
            {
                "username": "abcd"
            }
            """
        And show body received
        Then the client receives status code 201
        Then the client expect to receive an user

    Scenario: Update user (storage)
        When the client perform PUT request with body on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/abcd body:
            """
            {
                "username": "abcd",
                "mail": "abcd@test.fr"
            }
            """
        And show body received
        Then the client receives status code 200
        Then the client expect to receive an user


    Scenario: Update user not exist (storage)
        When the client perform PUT request with body on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/abcde body:
            """
            {
                "username": "abcde",
                "mail": "abcde@test.fr"
            }
            """
        And show body received
        Then the client receives status code 404

    Scenario: Delete user (storage)
        When the client perform DELETE request on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/abcd
        And show body received
        Then the client receives status code 204

    Scenario: Delete user not exist (storage)
        When the client perform DELETE request on url /realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/abcd
        And show body received
        Then the client receives status code 404
