targetScope = 'resourceGroup'

@description('Azure region of the existing Plan 086 Container Apps environment.')
param location string = 'eastasia'

@description('Existing ACR containing the verifier image.')
param acrName string
param acrResourceGroupName string

@description('Dedicated Key Vault and secret provisioned after foundation setup.')
param keyVaultName string
param keyVaultResourceGroupName string
param mysqlPasswordSecretName string

@description('Dedicated read-only MySQL verifier account and Plan 081 marker target.')
param mysqlHost string
param mysqlDatabase string
param mysqlUser string
param seedTargetId string

var acaEnvironmentName = 'acae-java-coban-rs-vnet-2026'

resource acaEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' existing = {
  name: acaEnvironmentName
}

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = {
  name: acrName
  scope: resourceGroup(acrResourceGroupName)
}

resource verifierJob 'Microsoft.App/jobs@2024-03-01' = {
  name: 'aca-job-java-coban-db-verify-2026'
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    environmentId: acaEnvironment.id
    workloadProfileName: 'Consumption'
    configuration: {
      triggerType: 'Manual'
      manualTriggerConfig: {
        parallelism: 1
        replicaCompletionCount: 1
      }
      replicaTimeout: 1800
      replicaRetryLimit: 0
      registries: [
        {
          server: acr.properties.loginServer
          identity: 'system'
        }
      ]
      secrets: [
        {
          name: 'mysql-password'
          keyVaultUrl: 'https://${keyVaultName}.${environment().suffixes.keyvaultDns}/secrets/${mysqlPasswordSecretName}'
          identity: 'system'
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'db-verify'
          image: 'mcr.microsoft.com/azure-cli:2.78.0'
          command: [
            '/bin/false'
          ]
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              name: 'MYSQL_HOST'
              value: mysqlHost
            }
            {
              name: 'MYSQL_DATABASE'
              value: mysqlDatabase
            }
            {
              name: 'MYSQL_USER'
              value: mysqlUser
            }
            {
              name: 'MYSQL_PWD'
              secretRef: 'mysql-password'
            }
            {
              name: 'SEED_TARGET_ID'
              value: seedTargetId
            }
          ]
        }
      ]
    }
  }
}

module verifierAcrPull 'modules/plan086-acr-pull-role.bicep' = {
  name: 'plan086VerifierAcrPull'
  scope: resourceGroup(acrResourceGroupName)
  params: {
    registryName: acrName
    principalId: verifierJob.identity.principalId
    assignmentName: guid(acr.id, verifierJob.id, 'AcrPull')
  }
}

module verifierKeyVaultReader 'modules/plan086-keyvault-secret-reader-role.bicep' = {
  name: 'plan086VerifierSecretReader'
  scope: resourceGroup(keyVaultResourceGroupName)
  params: {
    keyVaultName: keyVaultName
    secretName: mysqlPasswordSecretName
    principalId: verifierJob.identity.principalId
    assignmentName: guid(keyVaultName, mysqlPasswordSecretName, verifierJob.id, 'KeyVaultSecretUser')
  }
}

output verifierJobId string = verifierJob.id
output verifierJobName string = verifierJob.name
