targetScope = 'resourceGroup'

@description('Azure region shared by the VNet and Container Apps environment.')
param location string = 'eastasia'

@description('Address ranges must be selected after checking VNet and peering conflicts.')
param vnetAddressPrefix string = '10.42.0.0/16'

@description('Dedicated Container Apps workload profiles subnet. Microsoft recommends allowing room to grow.')
param acaSubnetPrefix string = '10.42.0.0/23'

@description('Dedicated subnet for private endpoints. Do not delegate this subnet.')
param privateEndpointSubnetPrefix string = '10.42.2.0/27'

@description('Resource ID of the existing Azure Database for MySQL Flexible Server.')
param mysqlServerResourceId string

var vnetName = 'vnet-java-coban-demo-2026'
var acaEnvironmentName = 'acae-java-coban-rs-vnet-2026'
var acaSubnetName = 'snet-aca-infra'
var privateEndpointSubnetName = 'snet-private-endpoints'
var privateDnsZoneName = 'privatelink.mysql.database.azure.com'
var logAnalyticsWorkspaceName = 'log-java-coban-demo-2026'

resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  properties: {
    retentionInDays: 30
    sku: {
      name: 'PerGB2018'
    }
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
  tags: {
    'plan086-purpose': 'aca-db-verifier-logs'
  }
}

resource vnet 'Microsoft.Network/virtualNetworks@2024-05-01' = {
  name: vnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [vnetAddressPrefix]
    }
  }
}

resource acaSubnet 'Microsoft.Network/virtualNetworks/subnets@2024-05-01' = {
  parent: vnet
  name: acaSubnetName
  properties: {
    addressPrefixes: [acaSubnetPrefix]
    delegations: [
      {
        name: 'Microsoft.App.environments'
        properties: {
          serviceName: 'Microsoft.App/environments'
        }
      }
    ]
  }
}

resource privateEndpointSubnet 'Microsoft.Network/virtualNetworks/subnets@2024-05-01' = {
  parent: vnet
  name: privateEndpointSubnetName
  properties: {
    addressPrefixes: [privateEndpointSubnetPrefix]
    privateEndpointNetworkPolicies: 'Disabled'
  }
}

resource acaEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: acaEnvironmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalyticsWorkspace.properties.customerId
        sharedKey: logAnalyticsWorkspace.listKeys().primarySharedKey
      }
    }
    vnetConfiguration: {
      infrastructureSubnetId: acaSubnet.id
      internal: false
    }
    workloadProfiles: [
      {
        name: 'Consumption'
        workloadProfileType: 'Consumption'
        minimumCount: 0
        maximumCount: 1
      }
    ]
  }
}

resource privateDnsZone 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: privateDnsZoneName
  location: 'global'
}

resource privateDnsVnetLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsZone
  name: 'link-vnet-java-coban-demo-2026'
  location: 'global'
  properties: {
    registrationEnabled: false
    virtualNetwork: {
      id: vnet.id
    }
  }
}

resource mysqlPrivateEndpoint 'Microsoft.Network/privateEndpoints@2024-05-01' = {
  name: 'pe-mysql-demo-2026'
  location: location
  properties: {
    subnet: {
      id: privateEndpointSubnet.id
    }
    privateLinkServiceConnections: [
      {
        name: 'mysql-flexible-server'
        properties: {
          privateLinkServiceId: mysqlServerResourceId
          groupIds: [
            'mysqlServer'
          ]
          requestMessage: 'Plan 086 private MySQL access from the demo VNet.'
        }
      }
    ]
  }
}

resource mysqlPrivateDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = {
  parent: mysqlPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'mysql'
        properties: {
          privateDnsZoneId: privateDnsZone.id
        }
      }
    ]
  }
}

output vnetId string = vnet.id
output acaInfrastructureSubnetId string = acaSubnet.id
output privateEndpointSubnetId string = privateEndpointSubnet.id
output mysqlPrivateEndpointId string = mysqlPrivateEndpoint.id
output mysqlPrivateDnsZoneId string = privateDnsZone.id

output acaEnvironmentId string = acaEnvironment.id


output logAnalyticsWorkspaceId string = logAnalyticsWorkspace.id
output logAnalyticsWorkspaceName string = logAnalyticsWorkspace.name
