/*
 * Copyright 2012-2018 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.hub.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.ConfigDir;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.client.ext.modulesloader.ssl.SimpleX509TrustManager;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubProject;
import com.marklogic.hub.error.DataHubConfigurationException;
import com.marklogic.hub.error.InvalidDBOperationError;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.ManageConfig;
import com.marklogic.mgmt.admin.AdminConfig;
import com.marklogic.mgmt.admin.AdminManager;
import org.apache.commons.text.CharacterPredicate;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC,
    getterVisibility = JsonAutoDetect.Visibility.ANY,
    setterVisibility = JsonAutoDetect.Visibility.ANY)
public class HubConfigImpl implements HubConfig {

    protected String stagingDbName = DEFAULT_STAGING_NAME;
    protected String stagingHttpName = DEFAULT_STAGING_NAME;
    protected Integer stagingForestsPerHost = DEFAULT_FORESTS_PER_HOST;
    protected Integer stagingPort = DEFAULT_STAGING_PORT;
    protected String stagingAuthMethod = DEFAULT_AUTH_METHOD;
    private String stagingScheme = DEFAULT_SCHEME;
    private boolean stagingSimpleSsl = false;
    private SSLContext stagingSslContext;
    private DatabaseClientFactory.SSLHostnameVerifier stagingSslHostnameVerifier;
    private String stagingCertFile;
    private String stagingCertPassword;
    private String stagingExternalName;
    private X509TrustManager stagingTrustManager;

    protected String finalDbName = DEFAULT_FINAL_NAME;
    protected String finalHttpName = DEFAULT_FINAL_NAME;
    protected Integer finalForestsPerHost = DEFAULT_FORESTS_PER_HOST;
    protected Integer finalPort = DEFAULT_FINAL_PORT;
    protected String finalAuthMethod = DEFAULT_AUTH_METHOD;
    private String finalScheme = DEFAULT_SCHEME;
//    private boolean finalSimpleSsl = false;
//    private SSLContext finalSslContext;
//    private DatabaseClientFactory.SSLHostnameVerifier finalSslHostnameVerifier;
//    private String finalCertFile;
//    private String finalCertPassword;
//    private String finalExternalName;

    protected String jobDbName = DEFAULT_JOB_NAME;
    protected String jobHttpName = DEFAULT_JOB_NAME;
    protected Integer jobForestsPerHost = 1;
    protected Integer jobPort = DEFAULT_JOB_PORT;
    protected String jobAuthMethod = DEFAULT_AUTH_METHOD;
    private String jobScheme = DEFAULT_SCHEME;
    private boolean jobSimpleSsl = false;
    private SSLContext jobSslContext;
    private DatabaseClientFactory.SSLHostnameVerifier jobSslHostnameVerifier;
    private String jobCertFile;
    private String jobCertPassword;
    private String jobExternalName;
    private X509TrustManager jobTrustManager;

//    protected String modulesDbName = DEFAULT_STAGING_MODULES_DB_NAME;
//    protected Integer modulesForestsPerHost = 1;
    protected String stagingModulesDbName = DEFAULT_STAGING_MODULES_DB_NAME;
    protected Integer stagingModulesForestsPerHost = 1;
    protected String finalModulesDbName = DEFAULT_FINAL_MODULES_DB_NAME;
    protected Integer finalModulesForestsPerHost = 1;


//    protected String triggersDbName = DEFAULT_TRIGGERS_DB_NAME;
//    protected Integer triggersForestsPerHost = 1;
    protected String stagingTriggersDbName = DEFAULT_STAGING_TRIGGERS_DB_NAME;
    protected Integer stagingTriggersForestsPerHost = 1;
    protected String finalTriggersDbName = DEFAULT_FINAL_TRIGGERS_DB_NAME;
    protected Integer finalTriggersForestsPerHost = 1;

//    protected String schemasDbName = DEFAULT_SCHEMAS_DB_NAME;
//    protected Integer schemasForestsPerHost = 1;
    protected String stagingSchemasDbName = DEFAULT_STAGING_SCHEMAS_DB_NAME;
    protected Integer stagingSchemasForestsPerHost = 1;
    protected String finalSchemasDbName = DEFAULT_FINAL_SCHEMAS_DB_NAME;
    protected Integer finalSchemasForestsPerHost = 1;

    private String hubRoleName = DEFAULT_ROLE_NAME;
    private String hubUserName = DEFAULT_USER_NAME;
    private String hubAdminRoleName = DEFAULT_ADMIN_ROLE_NAME;
    private String hubAdminUserName = DEFAULT_ADMIN_USER_NAME;

    // these hold runtime credentials for flows.
    private String mlUsername = null;
    private String mlPassword = null;

    private String[] loadBalancerHosts;

    protected String customForestPath = DEFAULT_CUSTOM_FOREST_PATH;
    protected String modulePermissions = "rest-reader,read,rest-writer,insert,rest-writer,update,rest-extension-user,execute";

    private String projectDir;

    private Properties environmentProperties;

    private HubProject hubProject;

    private ManageConfig manageConfig;
    private ManageClient manageClient;
    private AdminConfig adminConfig;
    private AdminManager adminManager;

    private AppConfig stagingAppConfig;
    private AppConfig finalAppConfig;

    private static final Logger logger = LoggerFactory.getLogger(HubConfigImpl.class);

    private ObjectMapper objmapper;

    public HubConfigImpl() {

        objmapper = new ObjectMapper();
    }

    public HubConfigImpl(String projectDir) {
        this();
        setProjectDir(new File(projectDir).getAbsolutePath());
    }


    public String getHost() { return stagingAppConfig.getHost(); }

    @Override public String getDbName(DatabaseKind kind){
        String name;
        switch (kind) {
            case STAGING:
                name = stagingDbName;
                break;
            case FINAL:
                name = finalDbName;
                break;
            case JOB:
                name = jobDbName;
                break;
            case TRACE:
                name = jobDbName;
                break;
            case STAGING_MODULES:
                name = stagingModulesDbName;
                break;
            case FINAL_MODULES:
                name = finalModulesDbName;
                break;
            case STAGING_TRIGGERS:
                name = stagingTriggersDbName;
                break;
            case FINAL_TRIGGERS:
                name = finalTriggersDbName;
                break;
            case STAGING_SCHEMAS:
                name = stagingSchemasDbName;
                break;
            case FINAL_SCHEMAS:
                name = finalSchemasDbName;
                break;
            default:
                throw new InvalidDBOperationError(kind, "grab database name");
        }
        return name;
    }

    @Override public void setDbName(DatabaseKind kind, String dbName){
        switch (kind) {
            case STAGING:
                stagingDbName = dbName;
                break;
            case FINAL:
                finalDbName = dbName;
                break;
            case JOB:
                jobDbName = dbName;
                break;
            case TRACE:
                jobDbName = dbName;
                break;
            case STAGING_MODULES:
                stagingModulesDbName = dbName;
                break;
            case FINAL_MODULES:
                finalModulesDbName = dbName;
                break;
            case STAGING_TRIGGERS:
                stagingTriggersDbName = dbName;
                break;
            case FINAL_TRIGGERS:
                finalTriggersDbName = dbName;
                break;
            case STAGING_SCHEMAS:
                stagingSchemasDbName = dbName;
                break;
            case FINAL_SCHEMAS:
                finalSchemasDbName = dbName;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set database name");
        }
    }

    @Override public String getHttpName(DatabaseKind kind){
        String name;
        switch (kind) {
            case STAGING:
                name = stagingHttpName;
                break;
            case FINAL:
                name = finalHttpName;
                break;
            case JOB:
                name = jobHttpName;
                break;
            case TRACE:
                name = jobHttpName;
                break;
            default:
                throw new InvalidDBOperationError(kind, "grab http name");
        }
        return name;
    }

    @Override public void setHttpName(DatabaseKind kind, String httpName){
        switch (kind) {
            case STAGING:
                stagingHttpName = httpName;
                break;
            case FINAL:
                finalHttpName = httpName;
                break;
            case JOB:
                jobHttpName = httpName;
                break;
            case TRACE:
                jobHttpName = httpName;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set http name");
        }
    }

    @Override public Integer getForestsPerHost(DatabaseKind kind){
        Integer forests;
        switch (kind) {
            case STAGING:
                forests = stagingForestsPerHost;
                break;
            case FINAL:
                forests = finalForestsPerHost;
                break;
            case JOB:
                forests = jobForestsPerHost;
                break;
            case TRACE:
                forests = jobForestsPerHost;
                break;
            case STAGING_MODULES:
                forests = stagingModulesForestsPerHost;
                break;
            case FINAL_MODULES:
                forests = finalModulesForestsPerHost;
                break;
            case STAGING_TRIGGERS:
                forests = stagingTriggersForestsPerHost;
                break;
            case FINAL_TRIGGERS:
                forests = finalTriggersForestsPerHost;
                break;
            case STAGING_SCHEMAS:
                forests = stagingSchemasForestsPerHost;
                break;
            case FINAL_SCHEMAS:
                forests = finalSchemasForestsPerHost;
                break;
            default:
                throw new InvalidDBOperationError(kind, "grab count of forests per host");
        }
        return forests;
    }

    @Override public void setForestsPerHost(DatabaseKind kind, Integer forestsPerHost){
        switch (kind) {
            case STAGING:
                stagingForestsPerHost = forestsPerHost;
                break;
            case FINAL:
                finalForestsPerHost = forestsPerHost;
                break;
            case JOB:
                jobForestsPerHost = forestsPerHost;
                break;
            case TRACE:
                jobForestsPerHost = forestsPerHost;
                break;
            case STAGING_MODULES:
                stagingModulesForestsPerHost = forestsPerHost;
                break;
            case FINAL_MODULES:
                finalModulesForestsPerHost = forestsPerHost;
                break;
            case STAGING_TRIGGERS:
                stagingTriggersForestsPerHost = forestsPerHost;
                break;
            case FINAL_TRIGGERS:
                finalTriggersForestsPerHost = forestsPerHost;
                break;
            case STAGING_SCHEMAS:
                stagingSchemasForestsPerHost = forestsPerHost;
                break;
            case FINAL_SCHEMAS:
                finalSchemasForestsPerHost = forestsPerHost;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set count of forests per host");
        }
    }

    @Override public Integer getPort(DatabaseKind kind){
        Integer port;
        switch (kind) {
            case STAGING:
                port = stagingPort;
                break;
            case FINAL:
                port = finalPort;
                break;
            case JOB:
                port = jobPort;
                break;
            case TRACE:
                port = jobPort;
                break;
            default:
                throw new InvalidDBOperationError(kind, "grab app port");
        }
        return port;
    }

    @Override public void setPort(DatabaseKind kind, Integer port){
        switch (kind) {
            case STAGING:
                stagingPort = port;
                break;
            case FINAL:
                finalPort = port;
                break;
            case JOB:
                jobPort = port;
                break;
            case TRACE:
                jobPort = port;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set app port");
        }
    }


    @Override public SSLContext getSslContext(DatabaseKind kind) {
        SSLContext sslContext;
        switch (kind) {
            case STAGING:
                sslContext = this.stagingSslContext;
                break;
            case JOB:
                sslContext = this.jobSslContext;
                break;
            case TRACE:
                sslContext = this.jobSslContext;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get ssl context");
        }
        return sslContext;
    }

    @Override public void setSslContext(DatabaseKind kind, SSLContext sslContext) {
        switch (kind) {
            case STAGING:
                this.stagingSslContext = sslContext;
                break;
            case JOB:
                this.jobSslContext = sslContext;
                break;
            case TRACE:
                this.jobSslContext = sslContext;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set ssl context");
        }
    }

    @Override public DatabaseClientFactory.SSLHostnameVerifier getSslHostnameVerifier(DatabaseKind kind) {
        DatabaseClientFactory.SSLHostnameVerifier sslHostnameVerifier;
        switch (kind) {
            case STAGING:
                sslHostnameVerifier = this.stagingSslHostnameVerifier;
                break;
            case JOB:
                sslHostnameVerifier = this.jobSslHostnameVerifier;
                break;
            case TRACE:
                sslHostnameVerifier = this.jobSslHostnameVerifier;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get ssl hostname verifier");
        }
        return sslHostnameVerifier;
    }

    @Override public void setSslHostnameVerifier(DatabaseKind kind, DatabaseClientFactory.SSLHostnameVerifier sslHostnameVerifier) {
        switch (kind) {
            case STAGING:
                this.stagingSslHostnameVerifier = sslHostnameVerifier;
                break;
            case JOB:
                this.jobSslHostnameVerifier = sslHostnameVerifier;
                break;
            case TRACE:
                this.jobSslHostnameVerifier = sslHostnameVerifier;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set ssl hostname verifier");
        }
    }

    @Override public String getAuthMethod(DatabaseKind kind){
        String authMethod;
        switch (kind) {
            case STAGING:
                authMethod = this.stagingAuthMethod;
                break;
            case FINAL:
                authMethod = this.finalAuthMethod;
                break;
            case JOB:
                authMethod = this.jobAuthMethod;
                break;
            case TRACE:
                authMethod = this.jobAuthMethod;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get auth method");
        }
        return authMethod;
    }

    @Override public void setAuthMethod(DatabaseKind kind, String authMethod) {
        switch (kind) {
            case STAGING:
                this.stagingAuthMethod = authMethod;
                break;
            case FINAL:
                this.finalAuthMethod = authMethod;
                break;
            case JOB:
                this.jobAuthMethod = authMethod;
                break;
            case TRACE:
                this.jobAuthMethod = authMethod;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set auth method");
        }
    }

    public X509TrustManager getTrustManager(DatabaseKind kind) {
        switch (kind) {
            case STAGING:
                return this.stagingTrustManager;
            case JOB:
                return this.jobTrustManager;
            default:
                throw new InvalidDBOperationError(kind, "set auth method");
        }
    }

    @Override
    public void setTrustManager(DatabaseKind kind, X509TrustManager trustManager) {
        switch (kind) {
            case STAGING:
                this.stagingTrustManager = trustManager;
                break;
            case JOB:
                this.jobTrustManager = trustManager;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set auth method");
        }
    }

    @Override public String getScheme(DatabaseKind kind){
        String scheme;
        switch (kind) {
            case STAGING:
                scheme = this.stagingScheme;
                break;
            case FINAL:
                scheme = this.finalScheme;
                break;
            case JOB:
                scheme = this.jobScheme;
                break;
            case TRACE:
                scheme = this.jobScheme;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get scheme");
        }
        return scheme;
    }

    @Override public void setScheme(DatabaseKind kind, String scheme) {
        switch (kind) {
            case STAGING:
                this.stagingScheme = scheme;
                break;
            case FINAL:
                this.finalScheme = scheme;
                break;
            case JOB:
                this.jobScheme = scheme;
                break;
            case TRACE:
                this.jobScheme = scheme;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set auth method");
        }
    }

    @Override public boolean getSimpleSsl(DatabaseKind kind){
        boolean simple;
        switch (kind) {
            case STAGING:
                simple = this.stagingSimpleSsl;
                break;
            case JOB:
                simple = this.jobSimpleSsl;
                break;
            case TRACE:
                simple = this.jobSimpleSsl;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get simple ssl");
        }
        return simple;
    }

    @Override public void setSimpleSsl(DatabaseKind kind, Boolean simpleSsl) {
        switch (kind) {
            case STAGING:
                this.stagingSimpleSsl = simpleSsl;
                break;
            case JOB:
                this.jobSimpleSsl = simpleSsl;
                break;
            case TRACE:
                this.jobSimpleSsl = simpleSsl;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set simple ssl");
        }
    }

    @Override public String getCertFile(DatabaseKind kind){
        String certFile;
        switch (kind) {
            case STAGING:
                certFile = this.stagingCertFile;
                break;
            case JOB:
                certFile = this.jobCertFile;
                break;
            case TRACE:
                certFile = this.jobCertFile;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get cert file");
        }
        return certFile;
    }

    @Override public void setCertFile(DatabaseKind kind, String certFile) {
        switch (kind) {
            case STAGING:
                this.stagingCertFile = certFile;
                break;
            case JOB:
                this.jobCertFile = certFile;
                break;
            case TRACE:
                this.jobCertFile = certFile;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set certificate file");
        }
    }

    @Override public String getCertPassword(DatabaseKind kind){
        String certPass;
        switch (kind) {
            case STAGING:
                certPass = this.stagingCertPassword;
                break;
            case JOB:
                certPass = this.jobCertPassword;
                break;
            case TRACE:
                certPass = this.jobCertPassword;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get cert password");
        }
        return certPass;
    }

    @Override public void setCertPass(DatabaseKind kind, String certPassword) {
        switch (kind) {
            case STAGING:
                this.stagingCertPassword = certPassword;
                break;
            case JOB:
                this.jobCertPassword = certPassword;
                break;
            case TRACE:
                this.jobCertPassword = certPassword;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set certificate password");
        }
    }

    @Override public String getExternalName(DatabaseKind kind){
        String name;
        switch (kind) {
            case STAGING:
                name = this.stagingExternalName;
                break;
            case JOB:
                name = this.jobExternalName;
                break;
            case TRACE:
                name = this.jobExternalName;
                break;
            default:
                throw new InvalidDBOperationError(kind, "get external name");
        }
        return name;
    }

    @Override public void setExternalName(DatabaseKind kind, String externalName) {
        switch (kind) {
            case STAGING:
                this.stagingExternalName = externalName;
                break;
            case JOB:
                this.jobExternalName = externalName;
                break;
            case TRACE:
                this.jobExternalName = externalName;
                break;
            default:
                throw new InvalidDBOperationError(kind, "set auth method");
        }
    }

    // roles and users
    @Override public String getHubRoleName() {
        return hubRoleName;
    }
    @Override public void setHubRoleName(String hubRoleName) {
        this.hubRoleName = hubRoleName;
    }

    @Override public String getHubUserName() {
        return hubUserName;
    }

    // impl only pending refactor to Flow Component
    public String getMlUsername() {
        return mlUsername;
    }
    // impl only pending refactor to Flow Component
    public String getMlPassword() {
        return mlPassword;
    }
    @Override  public void setHubUserName(String hubUserName) {
        this.hubUserName = hubUserName;
    }


    @JsonIgnore
    @Override  public String[] getLoadBalancerHosts() {
        return loadBalancerHosts;
    }
    public void setLoadBalancerHosts(String[] loadBalancerHosts) {
        this.loadBalancerHosts = loadBalancerHosts;
    }

    @Override public String getCustomForestPath() {
        return customForestPath;
    }
    public void setCustomForestPath(String customForestPath) {
        this.customForestPath = customForestPath;
    }

    @Override public String getModulePermissions() {
        return modulePermissions;
    }
    public void setModulePermissions(String modulePermissions) {
        this.modulePermissions = modulePermissions;
    }

    @Override public String getProjectDir() {
        return this.projectDir;
    }

    @Override public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
        this.hubProject = HubProject.create(projectDir);
    }

    @JsonIgnore
    @Override  public HubProject getHubProject() {
        return this.hubProject;
    }

    @Override  public void initHubProject() {
        this.hubProject.init(getCustomTokens());
    }

    @JsonIgnore
    @Override  public String getHubModulesDeployTimestampFile() {
        return Paths.get(projectDir, ".tmp", HUB_MODULES_DEPLOY_TIMESTAMPS_PROPERTIES).toString();
    }

    @JsonIgnore
    @Override public String getUserModulesDeployTimestampFile() {
        return Paths.get(projectDir, ".tmp", USER_MODULES_DEPLOY_TIMESTAMPS_PROPERTIES).toString();
    }

    @JsonIgnore
    public File getUserContentDeployTimestampFile() {
        return Paths.get(projectDir, ".tmp", USER_CONTENT_DEPLOY_TIMESTAMPS_PROPERTIES).toFile();
    }

    public void loadConfigurationFromProperties(Properties environmentProperties) {
        this.environmentProperties = environmentProperties;

        if (this.environmentProperties != null) {
            stagingDbName = getEnvPropString(environmentProperties, "mlStagingDbName", stagingDbName);
            stagingHttpName = getEnvPropString(environmentProperties, "mlStagingAppserverName", stagingHttpName);
            stagingForestsPerHost = getEnvPropInteger(environmentProperties, "mlStagingForestsPerHost", stagingForestsPerHost);
            stagingPort = getEnvPropInteger(environmentProperties, "mlStagingPort", stagingPort);
            stagingAuthMethod = getEnvPropString(environmentProperties, "mlStagingAuth", stagingAuthMethod);
            stagingScheme = getEnvPropString(environmentProperties, "mlStagingScheme", stagingScheme);
            stagingSimpleSsl = getEnvPropBoolean(environmentProperties, "mlStagingSimpleSsl", false);
            if (stagingSimpleSsl) {
                stagingSslContext = SimpleX509TrustManager.newSSLContext();
                stagingSslHostnameVerifier = DatabaseClientFactory.SSLHostnameVerifier.ANY;
                stagingTrustManager = new SimpleX509TrustManager();
            }
            stagingCertFile = getEnvPropString(environmentProperties, "mlStagingCertFile", stagingCertFile);
            stagingCertPassword = getEnvPropString(environmentProperties, "mlStagingCertPassword", stagingCertPassword);
            stagingExternalName = getEnvPropString(environmentProperties, "mlStagingExternalName", stagingExternalName);


            finalDbName = getEnvPropString(environmentProperties, "mlFinalDbName", finalDbName);
            finalHttpName = getEnvPropString(environmentProperties, "mlFinalAppserverName", finalHttpName);
            finalForestsPerHost = getEnvPropInteger(environmentProperties, "mlFinalForestsPerHost", finalForestsPerHost);
            finalPort = getEnvPropInteger(environmentProperties, "mlFinalPort", finalPort);
            finalAuthMethod = getEnvPropString(environmentProperties, "mlFinalAuth", finalAuthMethod);
            finalScheme = getEnvPropString(environmentProperties, "mlFinalScheme", finalScheme);
            // For 3.1 removed some properties that are not in use by DHF.  If DHF again needs the final appserver access in the future
            // (smart mastering?) then reincorporate the props here


            jobDbName = getEnvPropString(environmentProperties, "mlJobDbName", jobDbName);
            jobHttpName = getEnvPropString(environmentProperties, "mlJobAppserverName", jobHttpName);
            jobForestsPerHost = getEnvPropInteger(environmentProperties, "mlJobForestsPerHost", jobForestsPerHost);
            jobPort = getEnvPropInteger(environmentProperties, "mlJobPort", jobPort);
            jobAuthMethod = getEnvPropString(environmentProperties, "mlJobAuth", jobAuthMethod);
            jobScheme = getEnvPropString(environmentProperties, "mlJobScheme", jobScheme);
            jobSimpleSsl = getEnvPropBoolean(environmentProperties, "mlJobSimpleSsl", false);
            if (jobSimpleSsl) {
                jobSslContext = SimpleX509TrustManager.newSSLContext();
                jobSslHostnameVerifier = DatabaseClientFactory.SSLHostnameVerifier.ANY;
                jobTrustManager = new SimpleX509TrustManager();
            }
            jobCertFile = getEnvPropString(environmentProperties, "mlJobCertFile", jobCertFile);
            jobCertPassword = getEnvPropString(environmentProperties, "mlJobCertPassword", jobCertPassword);
            jobExternalName = getEnvPropString(environmentProperties, "mlJobExternalName", jobExternalName);

            customForestPath = getEnvPropString(environmentProperties, "mlCustomForestPath", customForestPath);

            stagingModulesDbName = getEnvPropString(environmentProperties, "mlStagingModulesDbName", stagingModulesDbName);
            stagingModulesForestsPerHost = getEnvPropInteger(environmentProperties, "mlStagingModulesForestsPerHost", stagingModulesForestsPerHost);
            modulePermissions = getEnvPropString(environmentProperties, "mlStagingModulePermissions", modulePermissions);

            finalModulesDbName = getEnvPropString(environmentProperties, "mlFinalModulesDbName", finalModulesDbName);
            finalModulesForestsPerHost = getEnvPropInteger(environmentProperties, "mlFinalModulesForestsPerHost", finalModulesForestsPerHost);
            modulePermissions = getEnvPropString(environmentProperties, "mlFinalModulePermissions", modulePermissions);

            stagingTriggersDbName = getEnvPropString(environmentProperties, "mlStagingTriggersDbName", stagingTriggersDbName);
            stagingTriggersForestsPerHost = getEnvPropInteger(environmentProperties, "mlStagingTriggersForestsPerHost", stagingTriggersForestsPerHost);

            finalTriggersDbName = getEnvPropString(environmentProperties, "mlFinalTriggersDbName", finalTriggersDbName);
            finalTriggersForestsPerHost = getEnvPropInteger(environmentProperties, "mlFinalTriggersForestsPerHost", finalTriggersForestsPerHost);

            stagingSchemasDbName = getEnvPropString(environmentProperties, "mlStagingSchemasDbName", stagingSchemasDbName);
            stagingSchemasForestsPerHost = getEnvPropInteger(environmentProperties, "mlStagingSchemasForestsPerHost", stagingSchemasForestsPerHost);

            finalSchemasDbName = getEnvPropString(environmentProperties, "mlFinalSchemasDbName", finalSchemasDbName);
            finalSchemasForestsPerHost = getEnvPropInteger(environmentProperties, "mlFinalSchemasForestsPerHost", finalSchemasForestsPerHost);

            hubRoleName = getEnvPropString(environmentProperties, "mlHubUserRole", hubRoleName);
            hubUserName = getEnvPropString(environmentProperties, "mlHubUserName", hubUserName);


            // this is a runtime username/password for running flows
            // could be factored away with FlowRunner
            mlUsername = getEnvPropString(environmentProperties, "mlUsername", mlUsername);
            mlPassword = getEnvPropString(environmentProperties, "mlPassword", mlPassword);

            String lbh = getEnvPropString(environmentProperties, "mlLoadBalancerHosts", null);
            if (lbh != null && lbh.length() > 0) {
                loadBalancerHosts = lbh.split(",");
            }

            projectDir = getEnvPropString(environmentProperties, "hubProjectDir", projectDir);

            logger.info("Hub Project Dir: " + projectDir);
        }
        else {
            logger.error("Missing environmentProperties");
        }
    }

    @JsonIgnore
    public ManageConfig getManageConfig() {
        return manageConfig;
    }
    public void setManageConfig(ManageConfig manageConfig) {
        this.manageConfig = manageConfig;
    }

    @JsonIgnore
    public ManageClient getManageClient() {
        return manageClient;
    }
    public void setManageClient(ManageClient manageClient) {
        this.manageClient = manageClient;
    }

    @JsonIgnore
    public AdminConfig getAdminConfig() { return adminConfig; }
    public void setAdminConfig(AdminConfig adminConfig) { this.adminConfig = adminConfig; }

    @JsonIgnore
    public AdminManager getAdminManager() {
        return adminManager;
    }
    public void setAdminManager(AdminManager adminManager) { this.adminManager = adminManager; }

    public DatabaseClient newAppServicesClient() {
        return getStagingAppConfig().newAppServicesDatabaseClient(stagingDbName);
    }

    @Override
    public DatabaseClient newStagingClient() {
        return newStagingClient(stagingDbName);
    }

    private DatabaseClient newStagingClient(String dbName) {
        AppConfig appConfig = getStagingAppConfig();
        DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), stagingPort, getMlUsername(), getMlPassword());
        config.setDatabase(dbName);
        config.setSecurityContextType(SecurityContextType.valueOf(stagingAuthMethod.toUpperCase()));
        config.setSslHostnameVerifier(stagingSslHostnameVerifier);
        config.setSslContext(stagingSslContext);
        config.setCertFile(stagingCertFile);
        config.setCertPassword(stagingCertPassword);
        config.setExternalName(stagingExternalName);
        config.setTrustManager(stagingTrustManager);
        return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
    }

    @Override
    public DatabaseClient newFinalClient() {
        return newFinalClient(finalDbName);
    }

    private DatabaseClient newFinalClient(String dbName) {
        AppConfig appConfig = getFinalAppConfig();
        DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), finalPort, getMlUsername(), getMlPassword());
        config.setDatabase(dbName);
        config.setSecurityContextType(SecurityContextType.valueOf(finalAuthMethod.toUpperCase()));
        config.setSslHostnameVerifier(stagingSslHostnameVerifier);
        config.setSslContext(stagingSslContext);
        config.setCertFile(stagingCertFile);
        config.setCertPassword(stagingCertPassword);
        config.setExternalName(stagingExternalName);
        config.setTrustManager(stagingTrustManager);
        return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
    }

    public DatabaseClient newJobDbClient() {
        AppConfig appConfig = getStagingAppConfig();
        DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), jobPort, mlUsername, mlPassword);
        config.setDatabase(jobDbName);
        config.setSecurityContextType(SecurityContextType.valueOf(jobAuthMethod.toUpperCase()));
        config.setSslHostnameVerifier(jobSslHostnameVerifier);
        config.setSslContext(jobSslContext);
        config.setCertFile(jobCertFile);
        config.setCertPassword(jobCertPassword);
        config.setExternalName(jobExternalName);
        config.setTrustManager(jobTrustManager);
        return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
    }

    public DatabaseClient newTraceDbClient() {
        return newJobDbClient();
    }

    public DatabaseClient newModulesDbClient() {
        AppConfig appConfig = getStagingAppConfig();
        DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), stagingPort, mlUsername, mlPassword);
        config.setDatabase(appConfig.getModulesDatabaseName());
        config.setSecurityContextType(SecurityContextType.valueOf(finalAuthMethod.toUpperCase()));
        //config.setSslHostnameVerifier(finalSslHostnameVerifier);
        //config.setSslContext(finalSslContext);
        //config.setCertFile(finalCertFile);
        //config.setCertPassword(finalCertPassword);
        //config.setExternalName(finalExternalName);
        //config.setTrustManager(finalTrustManager);
        return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
    }

    @JsonIgnore
    @Override public Path getHubPluginsDir() {
        return hubProject.getHubPluginsDir();
    }

    @JsonIgnore
    @Override public Path getHubEntitiesDir() { return hubProject.getHubEntitiesDir(); }

    @JsonIgnore
    @Override public Path getHubMappingsDir() { return hubProject.getHubMappingsDir(); }

    @JsonIgnore
    @Override public Path getHubConfigDir() {
        return hubProject.getHubConfigDir();
    }

    @JsonIgnore
    @Override public Path getHubDatabaseDir() {
        return hubProject.getHubDatabaseDir();
    }

    @JsonIgnore
    @Override public Path getHubServersDir() {
        return hubProject.getHubServersDir();
    }

    @JsonIgnore
    @Override public Path getHubSecurityDir() {
        return hubProject.getHubSecurityDir();
    }

    @JsonIgnore
    @Override public Path getUserSecurityDir() {
        return hubProject.getUserSecurityDir();
    }

    @JsonIgnore
    @Override public Path getUserConfigDir() {
        return hubProject.getUserConfigDir();
    }

    @JsonIgnore
    @Override public Path getUserDatabaseDir() {
        return hubProject.getUserDatabaseDir();
    }

    @JsonIgnore
    @Override public Path getEntityDatabaseDir() {
        return hubProject.getEntityDatabaseDir();
    }

    @JsonIgnore
    @Override public Path getUserServersDir() {
        return hubProject.getUserServersDir();
    }

    @JsonIgnore
    @Override public AppConfig getStagingAppConfig() {
        return stagingAppConfig;
    }


    @Override public void setStagingAppConfig(AppConfig config) {
        setStagingAppConfig(config, false);
    }

    @Override public void setStagingAppConfig(AppConfig config, boolean skipUpdate) {
        this.stagingAppConfig = config;
        if (!skipUpdate) {
            updateStagingAppConfig(this.stagingAppConfig);
        }
    }

    @JsonIgnore
    @Override public AppConfig getFinalAppConfig() {
        return finalAppConfig;
    }


    @Override public void setFinalAppConfig(AppConfig config) {
        setFinalAppConfig(config, false);
    }

    @Override public void setFinalAppConfig(AppConfig config, boolean skipUpdate) {
        this.finalAppConfig = config;
        if (!skipUpdate) {
            updateFinalAppConfig(this.finalAppConfig);
        }
    }

    @Override public String getJarVersion() {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.properties");
        try {
            properties.load(inputStream);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        String version = (String)properties.get("version");

        // this lets debug builds work from an IDE
        if (version.equals("${project.version}")) {
            version = "4.0.0";
        }
        return version;
    }


    private Map<String, String> getCustomTokens() {
        AppConfig appConfig = getStagingAppConfig();
        return getCustomTokens(appConfig, appConfig.getCustomTokens());
    }

    private Map<String, String> getCustomTokens(AppConfig appConfig, Map<String, String> customTokens) {
        customTokens.put("%%mlHost%%", appConfig.getHost());
        customTokens.put("%%mlStagingAppserverName%%", stagingHttpName);
        customTokens.put("\"%%mlStagingPort%%\"", stagingPort.toString());
        customTokens.put("%%mlStagingDbName%%", stagingDbName);
        customTokens.put("%%mlStagingForestsPerHost%%", stagingForestsPerHost.toString());
        customTokens.put("%%mlStagingAuth%%", stagingAuthMethod);

        customTokens.put("%%mlFinalAppserverName%%", finalHttpName);
        customTokens.put("\"%%mlFinalPort%%\"", finalPort.toString());
        customTokens.put("%%mlFinalDbName%%", finalDbName);
        customTokens.put("%%mlFinalForestsPerHost%%", finalForestsPerHost.toString());
        customTokens.put("%%mlFinalAuth%%", finalAuthMethod);


        customTokens.put("%%mlJobAppserverName%%", jobHttpName);
        customTokens.put("\"%%mlJobPort%%\"", jobPort.toString());
        customTokens.put("%%mlJobDbName%%", jobDbName);
        customTokens.put("%%mlJobForestsPerHost%%", jobForestsPerHost.toString());
        customTokens.put("%%mlJobAuth%%", jobAuthMethod);

        customTokens.put("%%mlStagingModulesDbName%%", stagingModulesDbName);
        customTokens.put("%%mlStagingModulesForestsPerHost%%", stagingModulesForestsPerHost.toString());

        customTokens.put("%%mlFinalModulesDbName%%", finalModulesDbName);
        customTokens.put("%%mlFinalModulesForestsPerHost%%", finalModulesForestsPerHost.toString());

        customTokens.put("%%mlStagingTriggersDbName%%", stagingTriggersDbName);
        customTokens.put("%%mlStagingTriggersForestsPerHost%%", stagingTriggersForestsPerHost.toString());

        customTokens.put("%%mlFinalTriggersDbName%%", finalTriggersDbName);
        customTokens.put("%%mlFinalTriggersForestsPerHost%%", finalTriggersForestsPerHost.toString());

        customTokens.put("%%mlStagingSchemasDbName%%", stagingSchemasDbName);
        customTokens.put("%%mlStagingSchemasForestsPerHost%%", stagingSchemasForestsPerHost.toString());

        customTokens.put("%%mlFinalSchemasDbName%%", finalSchemasDbName);
        customTokens.put("%%mlFinalSchemasForestsPerHost%%", finalSchemasForestsPerHost.toString());

        customTokens.put("%%mlHubUserRole%%", hubRoleName);
        customTokens.put("%%mlHubUserName%%", hubUserName);

        customTokens.put("%%mlHubAdminRole%%", hubAdminRoleName);
        customTokens.put("%%mlHubAdminUserName%%", hubAdminUserName);

        // random password for hub user
        RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder().withinRange(33, 126).filteredBy((CharacterPredicate) codePoint -> (codePoint != 92 && codePoint != 34)).build();
        customTokens.put("%%mlHubUserPassword%%", randomStringGenerator.generate(20));
        // and another random password for hub Admin User
        customTokens.put("%%mlHubAdminUserPassword%%", randomStringGenerator.generate(20));

        customTokens.put("%%mlCustomForestPath%%", customForestPath);

        if (environmentProperties != null) {
            Enumeration keyEnum = environmentProperties.propertyNames();
            while (keyEnum.hasMoreElements()) {
                String key = (String) keyEnum.nextElement();
                if (key.matches("^ml[A-Z].+") && !customTokens.containsKey(key)) {
                    customTokens.put("%%" + key + "%%", (String) environmentProperties.get(key));
                }
            }
        }

        return customTokens;
    }

    private void updateStagingAppConfig(AppConfig config) {
        config.setRestPort(stagingPort);

        config.setTriggersDatabaseName(stagingTriggersDbName);
        config.setSchemasDatabaseName(stagingSchemasDbName);
        config.setModulesDatabaseName(stagingModulesDbName);

        config.setReplaceTokensInModules(true);
        config.setUseRoxyTokenPrefix(false);
        config.setModulePermissions(modulePermissions);

        HashMap<String, Integer> forestCounts = new HashMap<>();
        forestCounts.put(stagingDbName, stagingForestsPerHost);
        forestCounts.put(finalDbName, finalForestsPerHost);
        forestCounts.put(jobDbName, jobForestsPerHost);
        forestCounts.put(stagingModulesDbName, stagingModulesForestsPerHost);
        forestCounts.put(stagingTriggersDbName, stagingTriggersForestsPerHost);
        forestCounts.put(stagingSchemasDbName, stagingSchemasForestsPerHost);
        config.setForestCounts(forestCounts);

        ConfigDir configDir = new ConfigDir(getUserConfigDir().toFile());
        config.setConfigDir(configDir);

        config.setSchemasPath(getUserConfigDir().resolve("schemas").toString());

        Map<String, String> customTokens = getCustomTokens(config, config.getCustomTokens());

        if (environmentProperties != null) {
            Enumeration keyEnum = environmentProperties.propertyNames();
            while (keyEnum.hasMoreElements()) {
                String key = (String) keyEnum.nextElement();
                if (key.matches("^ml[A-Z].+") && !customTokens.containsKey(key)) {
                    customTokens.put("%%" + key + "%%", (String) environmentProperties.get(key));
                }
            }
        }


        String version = getJarVersion();
        customTokens.put("%%mlHubVersion%%", version);

        stagingAppConfig = config;
    }

    private void updateFinalAppConfig(AppConfig config) {
        config.setRestPort(stagingPort);

        config.setTriggersDatabaseName(finalTriggersDbName);
        config.setSchemasDatabaseName(finalSchemasDbName);
        config.setModulesDatabaseName(finalModulesDbName);

        config.setReplaceTokensInModules(true);
        config.setUseRoxyTokenPrefix(false);
        config.setModulePermissions(modulePermissions);

        HashMap<String, Integer> forestCounts = new HashMap<>();
        forestCounts.put(stagingDbName, stagingForestsPerHost);
        forestCounts.put(finalDbName, finalForestsPerHost);
        forestCounts.put(jobDbName, jobForestsPerHost);
        forestCounts.put(finalModulesDbName, finalModulesForestsPerHost);
        forestCounts.put(finalTriggersDbName, finalTriggersForestsPerHost);
        forestCounts.put(finalSchemasDbName, finalSchemasForestsPerHost);
        config.setForestCounts(forestCounts);

        ConfigDir configDir = new ConfigDir(getUserConfigDir().toFile());
        config.setConfigDir(configDir);

        config.setSchemasPath(getUserConfigDir().resolve("schemas").toString());

        Map<String, String> customTokens = getCustomTokens(config, config.getCustomTokens());

        if (environmentProperties != null) {
            Enumeration keyEnum = environmentProperties.propertyNames();
            while (keyEnum.hasMoreElements()) {
                String key = (String) keyEnum.nextElement();
                if (key.matches("^ml[A-Z].+") && !customTokens.containsKey(key)) {
                    customTokens.put("%%" + key + "%%", (String) environmentProperties.get(key));
                }
            }
        }


        String version = getJarVersion();
        customTokens.put("%%mlHubVersion%%", version);

        finalAppConfig = config;
    }

    private String getEnvPropString(Properties environmentProperties, String key, String fallback) {
        String value = environmentProperties.getProperty(key);
        if (value == null) {
            value = fallback;
        }
        return value;
    }

    private int getEnvPropInteger(Properties environmentProperties, String key, int fallback) {
        String value = environmentProperties.getProperty(key);
        int res;
        if (value != null) {
            res = Integer.parseInt(value);
        }
        else {
            res = fallback;
        }
        return res;
    }

    private boolean getEnvPropBoolean(Properties environmentProperties, String key, boolean fallback) {
        String value = environmentProperties.getProperty(key);
        boolean res;
        if (value != null) {
            res = Boolean.parseBoolean(value);
        }
        else {
            res = fallback;
        }
        return res;
    }

    @JsonIgnore
    public String getInfo()
    {

        try {
            return objmapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        }
        catch(Exception e)
        {
            throw new DataHubConfigurationException("Your datahub configuration could not serialize");

        }

    }

    public String toString() {
        return getInfo();
    }
}
