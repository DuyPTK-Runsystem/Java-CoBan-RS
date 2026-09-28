#!/usr/bin/env bash
set -euo pipefail

stage="${PLAN086_DEPLOY_STAGE:-}"
case "$stage" in
  foundation|verifier) ;;
  *) echo 'Set PLAN086_DEPLOY_STAGE to foundation or verifier.' >&2; exit 2 ;;
esac

required=(DEMO_NETWORK_RESOURCE_GROUP)
if [[ "$stage" == 'foundation' ]]; then
  required+=(MYSQL_SERVER_RESOURCE_ID PLAN086_CIDR_APPROVED CIDR_INVENTORY_REFERENCE CIDR_APPROVED_BY)
else
  required+=(PLAN086_VERIFIER_STAGE_APPROVED MYSQL_SERVER_RESOURCE_ID DEMO_PRIVATE_ENDPOINT_SUBNET_ID ACR_NAME ACR_RESOURCE_GROUP KEY_VAULT_NAME KEY_VAULT_RESOURCE_GROUP DEMO_MYSQL_PASSWORD_SECRET_NAME DEMO_MYSQL_HOST DEMO_MYSQL_DATABASE DEMO_MYSQL_USER DEMO_SEED_TARGET_ID)
fi
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required Plan 086 %s-stage setting is missing: %s\n' "$stage" "$name" >&2
    exit 2
  fi
done

[[ "$MYSQL_SERVER_RESOURCE_ID" =~ ^/subscriptions/[^/]+/resourceGroups/[^/]+/providers/Microsoft\.DBforMySQL/flexibleServers/[^/]+$ ]] || { echo 'MYSQL_SERVER_RESOURCE_ID must identify an Azure Database for MySQL Flexible Server.' >&2; exit 2; }
LOCATION="${PLAN086_LOCATION:-eastasia}"
VNET_CIDR="${PLAN086_VNET_CIDR:-10.42.0.0/16}"
ACA_CIDR="${PLAN086_ACA_SUBNET_CIDR:-10.42.0.0/23}"
PE_CIDR="${PLAN086_PE_SUBNET_CIDR:-10.42.2.0/27}"
vnet_name='vnet-java-coban-demo-2026'
aca_env_name='acae-java-coban-rs-vnet-2026'
aca_subnet_name='snet-aca-infra'
pe_subnet_name='snet-private-endpoints'
pe_name='pe-mysql-demo-2026'
dns_zone='privatelink.mysql.database.azure.com'
workspace_name='log-java-coban-demo-2026'

if [[ "$stage" == 'foundation' ]]; then
  [[ "$PLAN086_CIDR_APPROVED" == 'true' ]] || { echo 'Foundation blocked: set PLAN086_CIDR_APPROVED=true only after VNet/peering address inventory and owner sign-off.' >&2; exit 1; }
  python3 - "$VNET_CIDR" "$ACA_CIDR" "$PE_CIDR" <<'PY'
import ipaddress
import json
import subprocess
import sys

vnet, aca, pe = [ipaddress.ip_network(value, strict=True) for value in sys.argv[1:4]]
if not vnet.is_private:
    raise SystemExit('VNet CIDR must use RFC1918 private address space.')
for label, subnet in [('ACA', aca), ('private endpoint', pe)]:
    if not subnet.subnet_of(vnet):
        raise SystemExit(f'{label} subnet {subnet} is outside VNet {vnet}.')
if aca.prefixlen > 23:
    raise SystemExit('Workload profiles ACA subnet requires at least /23 capacity (prefix /23 or shorter).')
if aca.overlaps(pe):
    raise SystemExit(f'ACA subnet {aca} overlaps private endpoint subnet {pe}.')
result = subprocess.run(['az', 'network', 'vnet', 'list', '--output', 'json'], check=True, capture_output=True, text=True)
target_name = 'vnet-java-coban-demo-2026'
target_seen = False
for existing in json.loads(result.stdout):
    existing_name = existing.get('name')
    prefixes = existing.get('addressSpace', {}).get('addressPrefixes') or []
    parsed_prefixes = {ipaddress.ip_network(prefix, strict=False) for prefix in prefixes}
    if existing_name == target_name:
        target_seen = True
        if parsed_prefixes != {vnet}:
            raise SystemExit(f'Existing target VNet {target_name} has address prefixes {sorted(map(str, parsed_prefixes))}; expected exactly {vnet}. Stop for operator review.')
        subnets = {subnet.get('name'): subnet for subnet in existing.get('subnets') or []}
        expected_subnets = {
            'snet-aca-infra': (aca, 'Microsoft.App/environments'),
            'snet-private-endpoints': (pe, None),
        }
        for subnet_name, (expected_prefix, expected_delegation) in expected_subnets.items():
            subnet = subnets.get(subnet_name)
            actual_prefixes = subnet.get('addressPrefixes') or ([subnet['addressPrefix']] if subnet.get('addressPrefix') else []) if subnet else []
            parsed_subnets = {ipaddress.ip_network(prefix, strict=False) for prefix in actual_prefixes}
            if parsed_subnets != {expected_prefix}:
                raise SystemExit(f'Existing target VNet subnet {subnet_name} has prefixes {sorted(map(str, parsed_subnets))}; expected exactly {expected_prefix}. Stop for operator review.')
            if expected_delegation:
                delegations = {item.get('serviceName') for item in subnet.get('delegations') or []}
                if expected_delegation not in delegations:
                    raise SystemExit(f'Existing target VNet subnet {subnet_name} is missing delegation {expected_delegation}. Stop for operator review.')
        continue
    for prefix in prefixes:
        if vnet.overlaps(ipaddress.ip_network(prefix, strict=False)):
            raise SystemExit(f'VNet {vnet} overlaps existing subscription VNet {existing_name}: {prefix}.')
print(f'CIDR preflight passed: VNet {vnet}; ACA {aca}; PE {pe}; visible subscription VNet ranges checked.')
PY
  workspace_json="$(az monitor log-analytics workspace list --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --output json)"
  existing_workspace="$(jq -c --arg name "$workspace_name" '[.[] | select(.name == $name)][0] // empty' <<< "$workspace_json")"
  if [[ -n "$existing_workspace" ]]; then
    jq -e --arg location "$LOCATION" '
      ((.location | ascii_downcase) == ($location | ascii_downcase)) and
      ((.sku.name // .properties.sku.name) == "PerGB2018") and
      (.properties.retentionInDays == 30) and
      (.properties.features.enableLogAccessUsingOnlyResourcePermissions == true) and
      (.tags["plan086-purpose"] == "aca-db-verifier-logs")
    ' <<< "$existing_workspace" >/dev/null || {
      echo "Existing Log Analytics workspace $workspace_name does not match approved Plan 086 settings; refusing to update it." >&2
      exit 1
    }
  fi
  template='infra/plan086-vnet.bicep'
  parameters=(
    location="$LOCATION"
    vnetAddressPrefix="$VNET_CIDR"
    acaSubnetPrefix="$ACA_CIDR"
    privateEndpointSubnetPrefix="$PE_CIDR"
    mysqlServerResourceId="$MYSQL_SERVER_RESOURCE_ID"
  )
else
  [[ "$PLAN086_VERIFIER_STAGE_APPROVED" == 'true' ]] || { echo 'Verifier stage blocked: set PLAN086_VERIFIER_STAGE_APPROVED=true only after DBA provisioning, secret creation, and billable-action approval.' >&2; exit 1; }
  vnet_json="$(az network vnet show --name "$vnet_name" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --output json)"
  subnet_id="$(az network vnet subnet show --name "$aca_subnet_name" --vnet-name "$vnet_name" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --query id --output tsv)"
  [[ -n "$subnet_id" && "$(az containerapp env show --name "$aca_env_name" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --query properties.vnetConfiguration.infrastructureSubnetId --output tsv | tr '[:upper:]' '[:lower:]')" == "${subnet_id,,}" ]] || { echo 'Verifier stage blocked: expected VNet-attached ACA environment is absent or mismatched.' >&2; exit 1; }
  jq -e --arg vnet "$VNET_CIDR" --arg aca "$ACA_CIDR" --arg pe "$PE_CIDR" '
    ([.addressSpace.addressPrefixes[] | select(. == $vnet)] | length == 1) and
    ([.subnets[] | select(.name == "snet-aca-infra" and ((.addressPrefixes // [.addressPrefix]) | index($aca)) and ([.delegations[].serviceName] | index("Microsoft.App/environments")))] | length == 1) and
    ([.subnets[] | select(.name == "snet-private-endpoints" and ((.addressPrefixes // [.addressPrefix]) | index($pe)))] | length == 1)
  ' <<< "$vnet_json" >/dev/null || { echo 'Verifier stage blocked: VNet/subnet topology does not match the approved CIDRs.' >&2; exit 1; }
  pe_json="$(az network private-endpoint show --name "$pe_name" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --output json)"
  jq -e --arg target "${MYSQL_SERVER_RESOURCE_ID,,}" --arg subnet "${DEMO_PRIVATE_ENDPOINT_SUBNET_ID:-}" '
    ((.privateLinkServiceConnections[0].privateLinkServiceId // .manualPrivateLinkServiceConnections[0].privateLinkServiceId | ascii_downcase) == $target) and
    ((.privateLinkServiceConnections[0].privateLinkServiceConnectionState.status // .manualPrivateLinkServiceConnections[0].privateLinkServiceConnectionState.status | ascii_downcase) == "approved") and
    ((.subnet.id | ascii_downcase) == ($subnet | ascii_downcase))
  ' <<< "$pe_json" >/dev/null || { echo 'Verifier stage blocked: MySQL PE is not approved or has the wrong target/subnet.' >&2; exit 1; }
  vnet_id="$(az network vnet show --name "$vnet_name" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --query id --output tsv)"
  dns_link="$(az network private-dns link vnet list --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --zone-name "$dns_zone" --query "[?virtualNetwork.id=='$vnet_id'].name | [0]" --output tsv)"
  [[ -n "$dns_link" && "$dns_link" != 'None' ]] || { echo 'Verifier stage blocked: MySQL private DNS zone is not linked to the VNet.' >&2; exit 1; }
  la_json="$(az monitor log-analytics workspace show --workspace-name "$workspace_name" --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" --output json)"
  jq -e --arg location "$LOCATION" '
    ((.location | ascii_downcase) == ($location | ascii_downcase)) and
    ((.sku.name // .properties.sku.name) == "PerGB2018") and
    (.properties.retentionInDays == 30) and
    (.properties.features.enableLogAccessUsingOnlyResourcePermissions == true) and
    (.tags["plan086-purpose"] == "aca-db-verifier-logs")
  ' <<< "$la_json" >/dev/null || { echo 'Verifier stage blocked: Plan 086 Log Analytics workspace is absent or mismatched.' >&2; exit 1; }
  az acr show --name "$ACR_NAME" --resource-group "$ACR_RESOURCE_GROUP" --output none
  kv_json="$(az keyvault show --name "$KEY_VAULT_NAME" --resource-group "$KEY_VAULT_RESOURCE_GROUP" --output json)"
  [[ "$(jq -r '.properties.enableRbacAuthorization // false' <<< "$kv_json")" == 'true' ]] || { echo 'Verifier stage requires Key Vault Azure RBAC authorization; refusing legacy access-policy mode.' >&2; exit 1; }
  [[ "$DEMO_MYSQL_PASSWORD_SECRET_NAME" =~ ^[A-Za-z0-9-]+$ ]] || { echo 'Verifier secret name must contain only letters, digits or hyphens.' >&2; exit 2; }
  secret_id="$(az keyvault secret list --vault-name "$KEY_VAULT_NAME" --query "[?name=='$DEMO_MYSQL_PASSWORD_SECRET_NAME'].id | [0]" --output tsv)"
  [[ -n "$secret_id" && "$secret_id" != 'None' ]] || { echo 'Verifier stage blocked: dedicated MySQL verifier password secret does not exist.' >&2; exit 1; }
  template='infra/plan086-vnet-verifier.bicep'
  parameters=(
    location="$LOCATION"
    acrName="$ACR_NAME"
    acrResourceGroupName="$ACR_RESOURCE_GROUP"
    keyVaultName="$KEY_VAULT_NAME"
    keyVaultResourceGroupName="$KEY_VAULT_RESOURCE_GROUP"
    mysqlPasswordSecretName="$DEMO_MYSQL_PASSWORD_SECRET_NAME"
    mysqlHost="$DEMO_MYSQL_HOST"
    mysqlDatabase="$DEMO_MYSQL_DATABASE"
    mysqlUser="$DEMO_MYSQL_USER"
    seedTargetId="$DEMO_SEED_TARGET_ID"
  )
fi

az deployment group create \
  --name "plan086-${stage}-$(date -u +%Y%m%d%H%M%S)" \
  --resource-group "$DEMO_NETWORK_RESOURCE_GROUP" \
  --template-file "$template" \
  --parameters "${parameters[@]}" \
  --output json > /tmp/plan086-network-deployment.json

outputs="$(jq -c '.properties.outputs' /tmp/plan086-network-deployment.json)"
printf '%s\n' "$outputs"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  jq -r 'to_entries[] | "\(.key)=\(.value.value)"' <<< "$outputs" >> "$GITHUB_OUTPUT"
fi
if [[ "$stage" == 'foundation' ]]; then
  printf 'Plan 086 foundation deployed. CIDR inventory: %s (approved by %s).\n' "$CIDR_INVENTORY_REFERENCE" "$CIDR_APPROVED_BY"
else
  echo 'Plan 086 verifier Job deployed after secret/RBAC/DBA prerequisites.'
fi
