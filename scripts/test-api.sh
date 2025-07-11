#!/bin/bash

echo "🧪 Testing Store Order Management API (No JWT)"
echo "=============================================="

BASE_URL="http://localhost:8080"

echo ""
echo "1. Health Check"
echo "---------------"
curl -s "${BASE_URL}/actuator/health" | jq '.'

echo ""
echo ""
echo "2. Create Order"
echo "---------------"
ORDER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      { "productId": 1, "quantity": 2 },
      { "productId": 2, "quantity": 1 }
    ]
  }')

echo "$ORDER_RESPONSE" | jq '.'

# Extract order code from response for further testing
ORDER_CODE=$(echo "$ORDER_RESPONSE" | jq -r '.[0].orderCode // empty')

if [ -n "$ORDER_CODE" ]; then
    echo ""
    echo ""
    echo "3. Get Order by Code: $ORDER_CODE"
    echo "--------------------------------"
    curl -s "${BASE_URL}/api/orders/${ORDER_CODE}" | jq '.'

    echo ""
    echo ""
    echo "4. Get User Orders"
    echo "------------------"
    curl -s "${BASE_URL}/api/orders?userId=1" | jq '.'

    echo ""
    echo ""
    echo "5. Process Payment"
    echo "------------------"
    PAYMENT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/payments" \
      -H "Content-Type: application/json" \
      -d "{
        \"orderCode\": \"$ORDER_CODE\",
        \"amount\": 2049.97,
        \"cardNumber\": \"1234567890123456\",
        \"cardHolderName\": \"Test User\",
        \"expiryDate\": \"12/25\",
        \"cvv\": \"123\"
      }")

    echo "$PAYMENT_RESPONSE" | jq '.'

    echo ""
    echo ""
    echo "6. Get Payment Status"
    echo "--------------------"
    curl -s "${BASE_URL}/api/payments/${ORDER_CODE}/status" | jq '.'

else
    echo ""
    echo "❌ Failed to create order or extract order code"
    echo "Response: $ORDER_RESPONSE"
fi

echo ""
echo ""
echo "✅ API Testing Complete!" 