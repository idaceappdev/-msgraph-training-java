// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

// <ImportSnippet>
package graphapponlytutorial;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import com.microsoft.graph.requests.UserCollectionPage;

import okhttp3.Request;
// </ImportSnippet>

public class Graph {
    // <AppOnyAuthConfigSnippet>
    private static Properties _properties;
    private static ClientSecretCredential _clientSecretCredential;
    private static GraphServiceClient<Request> _appClient;

    public static void initializeGraphForAppOnlyAuth(Properties properties) throws Exception {
        // Ensure properties isn't null
        if (properties == null) {
            throw new Exception("Properties cannot be null");
        }

        _properties = properties;

        if (_clientSecretCredential == null) {
            final String clientId = _properties.getProperty("app.clientId");
            final String tenantId = _properties.getProperty("app.tenantId");
            final String clientSecret = _properties.getProperty("app.clientSecret");

            _clientSecretCredential = new ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .tenantId(tenantId)
                    .clientSecret(clientSecret)
                    .build();
        }

        if (_appClient == null) {
            final TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
                    // Use the .default scope when using app-only auth
                    List.of("https://graph.microsoft.com/.default"), _clientSecretCredential);

            _appClient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .buildClient();
        }
    }
    // </AppOnyAuthConfigSnippet>

    // <GetAppOnlyTokenSnippet>
    public static String getAppOnlyToken() throws Exception {
        // Ensure credential isn't null
        if (_clientSecretCredential == null) {
            throw new Exception("Graph has not been initialized for app-only auth");
        }

        // Request the .default scope as required by app-only auth
        final String[] graphScopes = new String[] { "https://graph.microsoft.com/.default" };

        final TokenRequestContext context = new TokenRequestContext();
        context.addScopes(graphScopes);

        final AccessToken token = _clientSecretCredential.getToken(context).block();
        return token.getToken();
    }
    // </GetAppOnlyTokenSnippet>

    // <GetUsersSnippet>
    public static UserCollectionPage getUsers() throws Exception {
        // Ensure client isn't null
        if (_appClient == null) {
            throw new Exception("Graph has not been initialized for app-only auth");
        }

        return _appClient.users()
                .buildRequest()
                .select("displayName,id,mail")
                .top(25)
                .orderBy("displayName")
                .get();
    }
    // </GetUsersSnippet>

    public static void getGroups() {
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new HeaderOption("ConsistencyLevel", "eventual"));
        requestOptions.add(new QueryOption("$count", "true"));

        GroupCollectionPage userGroups = _appClient.users("5380331d-8073-4202-aab7-cd3cde276e84")
                .memberOfAsGroup()
                .buildRequest(requestOptions)
                .select("displayName,id,mail")
                .filter("startswith(displayName, 'c')")
                .orderBy("displayName")
                .get();

        List<Group> allGroupsList = new ArrayList<>();

        do {
            List<Group> currentPageGroup = userGroups.getCurrentPage();
            allGroupsList.addAll(currentPageGroup);
            GroupCollectionRequestBuilder nextPage = userGroups.getNextPage();
            userGroups = nextPage == null ? null : nextPage.buildRequest().get();
        } while (userGroups != null);

        System.out.println("Total Group count is :" + allGroupsList.size());

        for (Group usergroup : allGroupsList) {
            System.out.println("  User Group Name: " + usergroup.displayName);
            System.out.println("  ID: " + usergroup.id);
            System.out.println("  Email: " + usergroup.mail);
        }
    }
}