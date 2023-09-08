#Script Name - send-msg-2-entity.sh

RESOURCE_URI=$1
SHARED_ACCESS_KEY_NAME=$2
SHARED_ACCESS_KEY=$3
ENTITY_NAME=$4

get_sas_token() {
    local EXPIRY=${EXPIRY:=$((60 * 60 * 24))} # Default token expiry is 1 day

    local ENCODED_URI=$(echo -n $RESOURCE_URI | jq -s -R -r @uri)
    local TTL=$(($(date +%s) + $EXPIRY))
    local UTF8_SIGNATURE=$(printf "%s\n%s" $ENCODED_URI $TTL | iconv -t utf8)

    local HASH=$(echo -n "$UTF8_SIGNATURE" | openssl sha256 -hmac $SHARED_ACCESS_KEY -binary | base64)
    local ENCODED_HASH=$(echo -n $HASH | jq -s -R -r @uri)

    echo -n "SharedAccessSignature sr=$ENCODED_URI&sig=$ENCODED_HASH&se=$TTL&skn=$SHARED_ACCESS_KEY_NAME"
}

SAS_TOKEN=$(get_sas_token)
curl --location "${RESOURCE_URI}${ENTITY_NAME}/messages" \
--header "Authorization: $SAS_TOKEN" \
--header 'BrokerProperties: {"Header1":"Value1","Header2":"Value2","Header3":"Value3"}' \
--header 'Content-Type: application/json' \
--data '{
  "What its doing": "Sending msg to service bus"
}'