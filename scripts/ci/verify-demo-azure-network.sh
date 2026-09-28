#!/usr/bin/env bash
set -euo pipefail

for name in AZURE_RESOURCE_GROUP ACA_NAME ACA_INFRA_SUBNET_ID ACA_JOB_NAME MYSQL_SERVER_RESOURCE_ID MYSQL_PRIVATE_ENDPOINT_NAME MYSQL_PRIVATE_ENDPOINT_SUBNET_ID DEMO_NETWORK_RESOURCE_GROUP NETWORK_STAGE; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required Azure network setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

case "$NETWORK_STAGE" in
  preflight|postcutover) ;;
  *) echo 'NETWORK_STAGE must be preflight or postcutover.' >&2; exit 2 ;;
esac

fail() {
  printf 'Azure private-network verification failed: %s\n' "$1" >&2
  exit 1
}

app_environment_id="$(az containerapp show --name "$ACA_NAME" --resource-group "$AZURE_RESOURCE_GROUP" --query properties.managedEnvironmentId --output tsv)"
[[ -n "$app_environment_id" && "$app_environment_id" != 'None' ]] || fail 'Container App has no managed environment ID.'
aca_subnet_id="$(az containerapp env show --ids "$app_environment_id" --query properties.vnetConfiguration.infrastructureSubnetId --output tsv)"
[[ "${aca_subnet_id,,}" == "${ACA_INFRA_SUBNET_ID,,}" ]] || fail 'Container Apps environment is not attached to the configured VNet subnet.'

job_environment_id="$(az containerapp job show --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" --query properties.environmentId --output tsv)"
[[ "${job_environment_id,,}" == "${app_environment_id,,}" ]] || fail 'The DB verifier Job does not share the application Container Apps environment.'
job_identity="$(az containerapp job show --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" --query identity.type --output tsv)"
[[ "$job_identity" == 'SystemAssigned' ]] || fail 'The DB verifier Job must use its own system-assigned managed identity.'
secret_uri="$(az containerapp job show --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" --query "properties.configuration.secrets[?name=='mysql-password'].keyVaultUrl | [0]" --output tsv)"
[[ "$secret_uri" == https://*.vault.azure.net/secrets/* ]] || fail 'The Job MySQL password secret is not referenced from Azure Key Vault.'

pe_json="$(az network private-endpoint show --name "$MYSQL_PRIVATE_ENDPOINT_NAME" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --output json)"
pe_target="$(jq -r '.privateLinkServiceConnections[0].privateLinkServiceId // .manualPrivateLinkServiceConnections[0].privateLinkServiceId // empty' <<< "$pe_json")"
pe_state="$(jq -r '.privateLinkServiceConnections[0].privateLinkServiceConnectionState.status // .manualPrivateLinkServiceConnections[0].privateLinkServiceConnectionState.status // empty' <<< "$pe_json")"
pe_subnet="$(jq -r '.subnet.id // empty' <<< "$pe_json")"
[[ "${pe_target,,}" == "${MYSQL_SERVER_RESOURCE_ID,,}" ]] || fail 'Private Endpoint targets a different MySQL server.'
[[ "${pe_state,,}" == 'approved' ]] || fail "Private Endpoint state is '$pe_state', expected Approved."
[[ "${pe_subnet,,}" == "${MYSQL_PRIVATE_ENDPOINT_SUBNET_ID,,}" ]] || fail 'Private Endpoint is attached to a different subnet.'

nic_id="$(jq -r '.networkInterfaces[0].id // empty' <<< "$pe_json")"
[[ -n "$nic_id" ]] || fail 'Private Endpoint has no network interface.'
private_ip="$(az network nic show --ids "$nic_id" --query ipConfigurations[0].privateIPAddress --output tsv)"
[[ -n "$private_ip" && "$private_ip" != 'None' ]] || fail 'Private Endpoint has no allocated private IP.'

vnet_id="${ACA_INFRA_SUBNET_ID%/subnets/*}"
dns_link="$(az network private-dns link vnet list --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --zone-name privatelink.mysql.database.azure.com --query "[?virtualNetwork.id=='$vnet_id'].name | [0]" --output tsv)"
[[ -n "$dns_link" && "$dns_link" != 'None' ]] || fail 'MySQL Private DNS zone is not linked to the Container Apps VNet.'

server_json="$(az mysql flexible-server show --ids "$MYSQL_SERVER_RESOURCE_ID" --output json)"
public_access="$(jq -r '.network.publicNetworkAccess // .publicNetworkAccess // empty' <<< "$server_json")"
if [[ "$NETWORK_STAGE" == 'postcutover' ]]; then
  [[ "${public_access,,}" == 'disabled' ]] || fail "Post-cutover public access is '$public_access', expected Disabled."
  server_name="${MYSQL_SERVER_RESOURCE_ID##*/}"
  mysql_group="$(sed -n 's#^/subscriptions/[^/]*/resourceGroups/\([^/]*\)/.*#\1#p' <<< "$MYSQL_SERVER_RESOURCE_ID")"
  [[ -n "$mysql_group" ]] || fail 'Could not determine MySQL resource group from resource ID.'
  firewall_rules="$(az mysql flexible-server firewall-rule list --name "$server_name" --resource-group "$mysql_group" --output json)"
  if jq -e 'any(.[]; (.startIpAddress == "0.0.0.0" and .endIpAddress == "255.255.255.255") or (.startIpAddress == "0.0.0.0" and .endIpAddress == "0.0.0.0"))' <<< "$firewall_rules" >/dev/null; then
    fail 'A broad allow-all/Azure-services firewall rule remains after cutover.'
  fi
fi

[[ -n "${GITHUB_OUTPUT:-}" ]] || fail 'GITHUB_OUTPUT is unavailable; cannot pass the verified PE address to the Job.'
printf 'mysql_private_endpoint_ip=%s\n' "$private_ip" >> "$GITHUB_OUTPUT"
printf 'Azure network control plane verified (%s): app/job environment subnet=%s, PE=%s, private IP=%s, DNS link=%s, MySQL public access=%s.\n' \
  "$NETWORK_STAGE" "$aca_subnet_id" "$MYSQL_PRIVATE_ENDPOINT_NAME" "$private_ip" "$dns_link" "$public_access"
