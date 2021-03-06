/*
 * Copyright 2017 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.hub.api.gitlab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.apicurio.hub.api.beans.ApiDesignResourceInfo;
import io.apicurio.hub.api.beans.Collaborator;
import io.apicurio.hub.api.beans.GitLabGroup;
import io.apicurio.hub.api.beans.GitLabProject;
import io.apicurio.hub.api.beans.ResourceContent;
import io.apicurio.hub.api.config.HubApiConfiguration;
import io.apicurio.hub.api.connectors.SourceConnectorException;
import io.apicurio.hub.api.exceptions.NotFoundException;
import test.io.apicurio.hub.api.MockSecurityContext;
import test.io.apicurio.hub.api.TestUtil;

/**
 * @author eric.wittmann@gmail.com
 */
public class GitLabSourceConnectorTest {
    
    private static String pat = null;
    
    private IGitLabSourceConnector service;
    private HubApiConfiguration config;
    

    @Before
    public void setUp() {
        service = new GitLabSourceConnector() {
            @Override
            protected String getExternalToken() throws SourceConnectorException {
                try {
                    if (pat == null) {
                        // Read the Personal Access Token from standard input so we don't accidentally check it in.
                        // This is a PITA because we have to copy/paste our PAT every time we run this test.  But it's 
                        // better than accidentally checking in a GitLab PAT!!
                        System.out.println("Enter your GitLab Personal Access Token:");
                        pat = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    }
                    return pat;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            
            /**
             * @see io.apicurio.hub.api.gitlab.GitLabSourceConnector#getExternalTokenType()
             */
            @Override
            protected Object getExternalTokenType() {
                return GitLabSourceConnector.TOKEN_TYPE_PAT;
            }
        };
        config = new HubApiConfiguration();
        
        TestUtil.setPrivateField(service, "security", new MockSecurityContext());
        TestUtil.setPrivateField(service, "config", config);
    }
    
    @After
    public void tearDown() throws Exception {
    }

    @Test
    @Ignore
    public void testParseExternalTokenResponse() {
        String tokenBody = "{\r\n" + 
                "    \"access_token\": \"XYZ\",\r\n" + 
                "    \"token_type\": \"bearer\",\r\n" + 
                "    \"refresh_token\": \"ABC\",\r\n" + 
                "    \"scope\": \"openid read_user api\",\r\n" + 
                "    \"created_at\": 1508504375,\r\n" + 
                "    \"id_token\": \"123\"\r\n" + 
                "}";
        Map<String, String> response = ((GitLabSourceConnector) service).parseExternalTokenResponse(tokenBody);
        Assert.assertNotNull(response);
        Assert.assertEquals("XYZ", response.get("access_token"));
        Assert.assertEquals("openid read_user api", response.get("scope"));
        Assert.assertEquals("bearer", response.get("token_type"));
    }

    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#validateResourceExists(java.lang.String)}.
     */
    @Test
    @Ignore
    public void testValidateResourceExists() throws NotFoundException, SourceConnectorException {
        ApiDesignResourceInfo info = service.validateResourceExists("https://gitlab.com/Apicurio/api-samples/blob/master/3.0/simple-api.json");
        Assert.assertNotNull(info);
        Assert.assertEquals("Simple OAI 3.0.0 API", info.getName());
        Assert.assertEquals("A simple API using OpenAPI 3.0.0.", info.getDescription());

        try {
			info = service.validateResourceExists("https://gitlab.com/Apicurio/api-samples/blob/master/2.0/pet-store-missing.json");
			Assert.fail("Expected a NotFoundException");
		} catch (NotFoundException e) {
		}
    }

    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#getCollaborators(String)}.
     */
    @Test
    @Ignore
    public void testGetCollaborators() throws NotFoundException, SourceConnectorException {
        Collection<Collaborator> collaborators = service.getCollaborators("https://gitlab.com/Apicurio/api-samples/blob/master/3.0/simple-api.json");
        Assert.assertNotNull(collaborators);
        Assert.assertFalse(collaborators.isEmpty());
        Assert.assertTrue(collaborators.size() > 0);
        Collaborator collaborator = collaborators.iterator().next();
        Assert.assertTrue(collaborator.getCommits() > 0);
        Assert.assertNotNull(collaborator.getName());
        Assert.assertNotNull(collaborator.getUrl());

        // TODO enable this once we move to a commit-based analysis for returning collaborator info
//        try {
//			service.getCollaborators("https://gitlab.com/Apicurio/api-samples/blob/master/2.0/pet-store-missing.json");
//			Assert.fail("Expected NotFoundException");
//		} catch (NotFoundException e) {
//		}
    }

    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#getResourceContent(String)}.
     */
    @Test
    @Ignore
    public void testGetResourceContent() throws NotFoundException, SourceConnectorException {
        ResourceContent content = service.getResourceContent("https://gitlab.com/Apicurio/api-samples/blob/master/pet-store.json");
        Assert.assertTrue(content.getContent().contains("Swagger Petstore"));
        Assert.assertNotNull(content.getSha());
        
        try {
			service.getResourceContent("https://gitlab.com/Apicurio/api-samples/blob/master/2.0/pet-store-missing.json");
			Assert.fail("Expected NotFoundException");
		} catch (NotFoundException e) {
		}
    }

    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#getGroups()}.
     */
    @Test
    @Ignore
    public void testGetGroups() throws GitLabException, SourceConnectorException {
        Collection<GitLabGroup> groups = service.getGroups();
        Assert.assertNotNull(groups);
        Assert.assertTrue(groups.size() > 0);
        System.out.println("Found " + groups.size() + " groups!");
        groups.forEach( group -> {
            System.out.println("\t" + group.getName());
        });
    }

    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#getProjects(String)}.
     */
    @Test
    @Ignore
    public void testGetProjects() throws GitLabException, SourceConnectorException {
        Collection<GitLabProject> projects = service.getProjects("apicurio");
        Assert.assertNotNull(projects);
        System.out.println("Found " + projects.size() + " projects!");
        projects.forEach( project -> {
            System.out.println("\t" + project.getName());
        });
        Assert.assertTrue(projects.size() > 0);
    }


    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#getResourceContent(String)}.
     */
    @Test
    @Ignore
    public void testUpdateResourceContent() throws NotFoundException, SourceConnectorException, JsonProcessingException, IOException {
        String repositoryUrl = "https://gitlab.com/Apicurio/api-samples/blob/master/animation/animation-api.json";
        
        ResourceContent content = service.getResourceContent(repositoryUrl);
        Assert.assertTrue(content.getContent().contains("Animation API"));
        Assert.assertNotNull(content.getSha());
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content.getContent());
        ObjectNode info = (ObjectNode) root.get("info");
        info.set("version", TextNode.valueOf(String.valueOf(System.currentTimeMillis())));
        
        String newContent = mapper.writeValueAsString(root);
        content.setContent(newContent);
        String newSha = service.updateResourceContent(repositoryUrl, "Unit Test: Update Content", "Updated the version of: " + repositoryUrl, content);
        System.out.println("New SHA: " + newSha);
    }

    /**
     * Test method for {@link io.apicurio.hub.api.gitlab.GitLabSourceConnector#createResourceContent(String, String, String)
     */
    @Test
    @Ignore
    public void testCreateResourceContent() throws NotFoundException, SourceConnectorException, JsonProcessingException, IOException {
        String repositoryUrl = "https://gitlab.com/Apicurio/api-samples/blob/master/junit/api-" + System.currentTimeMillis() + ".json";
        String content = "{ \"swagger\" : \"2.0\" }";
        service.createResourceContent(repositoryUrl, "Created resource (junit)", content);
    }

}
