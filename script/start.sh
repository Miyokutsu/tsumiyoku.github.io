#!/bin/bash

# Make script stop on first error
set -e

echo "🚀 Starting Tsumiyoku application..."

# Check if .env file exists, create it if not
if [ ! -f .env ]; then
    echo "Creating .env file with default development values..."
    cat > .env << EOL
MFA_TOTP_ENC_KEY=$(openssl rand -base64 32)
DISCORD_CLIENT_ID=your_discord_client_id
DISCORD_CLIENT_SECRET=your_discord_client_secret
AUDIT_HMAC_KEY=$(openssl rand -base64 32)
EOL
    echo "⚠️  Please update DISCORD_CLIENT_ID and DISCORD_CLIENT_SECRET in .env file"
fi

# Build and start the containers
echo "🏗️  Building and starting containers..."
docker-compose build
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
timeout 30s bash -c 'until docker-compose ps | grep -q "app.*running"; do sleep 1; done'

echo "✅ Application is ready!"
echo "📝 Access the application at: http://localhost:8080"
echo "🗄️  Database is available at: localhost:5432"
echo "   - Database: tsumiyoku"
echo "   - Username: state_user"
echo "   - Password: state_pass"