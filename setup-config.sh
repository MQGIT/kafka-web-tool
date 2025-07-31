#!/bin/bash

# Kafka Web Tool - Configuration Setup Script
# This script helps users configure the application for their environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1"
}

# Banner
show_banner() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║                    🔧 Kafka Web Tool                        ║${NC}"
    echo -e "${BLUE}║                 Configuration Setup                         ║${NC}"
    echo -e "${BLUE}║                                                              ║${NC}"
    echo -e "${BLUE}║  Configure the application for your environment             ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Collect configuration
collect_configuration() {
    log_step "Configuration Collection"
    echo ""
    
    # Docker Registry
    echo "📦 Docker Registry Configuration"
    echo "This is where your Docker images will be stored and pulled from."
    echo ""
    echo "Examples:"
    echo "  - DockerHub: your-dockerhub-username"
    echo "  - Google GCR: gcr.io/your-project-id"
    echo "  - AWS ECR: 123456789012.dkr.ecr.us-west-2.amazonaws.com"
    echo "  - Azure ACR: yourregistry.azurecr.io"
    echo ""
    read -p "Enter your Docker registry: " REGISTRY
    
    if [ -z "$REGISTRY" ]; then
        log_error "Docker registry is required"
        exit 1
    fi
    
    # Application Hostname
    echo ""
    echo "🌐 Application Hostname"
    echo "This is the domain where your application will be accessible."
    echo ""
    echo "Examples:"
    echo "  - kafka-tool.yourcompany.com"
    echo "  - kafka.example.org"
    echo "  - kafka-dev.mydomain.net"
    echo ""
    read -p "Enter your application hostname: " HOSTNAME
    
    if [ -z "$HOSTNAME" ]; then
        log_error "Application hostname is required"
        exit 1
    fi
    
    # Namespace (optional)
    echo ""
    echo "🏷️  Kubernetes Namespace (optional)"
    echo "Default: kafka-tool"
    read -p "Enter Kubernetes namespace [kafka-tool]: " NAMESPACE
    if [ -z "$NAMESPACE" ]; then
        NAMESPACE="kafka-tool"
    fi
    
    echo ""
    log_info "Configuration Summary:"
    echo "  Registry: $REGISTRY"
    echo "  Hostname: $HOSTNAME"
    echo "  Namespace: $NAMESPACE"
    echo ""
    
    read -p "Continue with this configuration? (y/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Configuration cancelled"
        exit 0
    fi
}

# Update configuration files
update_configuration() {
    log_step "Updating Configuration Files"
    
    # Create backup directory
    BACKUP_DIR="config-backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    log_info "Creating backup in $BACKUP_DIR"
    
    # Files to update
    FILES=(
        "k8s/ingress.yaml"
        "k8s/nginx-config.yaml"
        "k8s/configmap.yaml"
        "backend/src/main/resources/application.yml"
        "backend/src/main/java/org/marsem/kafka/controller/HealthController.java"
        "quick-deploy.sh"
        "build-deploy.sh"
        "deploy.sh"
    )
    
    # Backup original files
    for file in "${FILES[@]}"; do
        if [ -f "$file" ]; then
            cp "$file" "$BACKUP_DIR/"
            log_info "Backed up $file"
        fi
    done
    
    # Update files
    log_info "Updating configuration files..."
    
    # Update registry references
    find . -name "*.yaml" -o -name "*.yml" -o -name "*.sh" -o -name "*.java" | \
        grep -E "(k8s/|backend/|quick-deploy.sh|build-deploy.sh|deploy.sh)" | \
        xargs sed -i.tmp \
            -e "s/your-registry/$REGISTRY/g" \
            -e "s/your-hostname\.com/$HOSTNAME/g"
    
    # Clean up temporary files
    find . -name "*.tmp" -delete
    
    log_success "Configuration files updated"
}

# Validate configuration
validate_configuration() {
    log_step "Validating Configuration"
    
    # Check if Docker registry is accessible
    log_info "Checking Docker registry access..."
    if docker info &> /dev/null; then
        log_success "Docker is running"
    else
        log_warning "Docker is not running or not accessible"
    fi
    
    # Check if hostname is valid
    log_info "Validating hostname format..."
    if [[ $HOSTNAME =~ ^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]*\.[a-zA-Z]{2,}$ ]]; then
        log_success "Hostname format is valid"
    else
        log_warning "Hostname format may not be valid"
    fi
    
    # Check if kubectl is available
    log_info "Checking Kubernetes access..."
    if kubectl cluster-info &> /dev/null; then
        log_success "Kubernetes cluster is accessible"
    else
        log_warning "Kubernetes cluster is not accessible"
    fi
}

# Generate environment file
generate_env_file() {
    log_step "Generating Environment File"
    
    cat > .env << EOF
# Kafka Web Tool Configuration
# Generated on $(date)

# Docker Registry
REGISTRY=$REGISTRY

# Application Configuration
HOSTNAME=$HOSTNAME
NAMESPACE=$NAMESPACE

# Build Configuration
BUILD_TAG=latest

# Usage:
# source .env && ./quick-deploy.sh
# source .env && ./build-deploy.sh
EOF
    
    log_success "Environment file created: .env"
    echo ""
    echo "You can now use:"
    echo "  source .env && ./quick-deploy.sh"
    echo "  source .env && ./build-deploy.sh"
}

# Main function
main() {
    show_banner
    
    log_info "This script will configure the Kafka Web Tool for your environment."
    echo ""
    
    collect_configuration
    update_configuration
    validate_configuration
    generate_env_file
    
    echo ""
    log_success "🎉 Configuration completed successfully!"
    echo ""
    echo "Next steps:"
    echo "1. Review the updated configuration files"
    echo "2. Test Docker registry access: docker login $REGISTRY"
    echo "3. Ensure DNS points $HOSTNAME to your Kubernetes ingress"
    echo "4. Deploy the application:"
    echo "   source .env && ./quick-deploy.sh"
    echo ""
    echo "For more information, see:"
    echo "  - CONFIGURATION_TEMPLATE.md"
    echo "  - QUICK_DEPLOY_GUIDE.md"
    echo "  - BUILD_DEPLOY_GUIDE.md"
    echo ""
}

# Parse command line arguments
case "${1:-setup}" in
    "setup")
        main
        ;;
    "restore")
        if [ -z "$2" ]; then
            echo "Usage: $0 restore <backup-directory>"
            exit 1
        fi
        log_info "Restoring configuration from $2"
        cp "$2"/* ./ 2>/dev/null || true
        log_success "Configuration restored"
        ;;
    *)
        echo "Usage: $0 {setup|restore}"
        echo "  setup   - Configure the application (default)"
        echo "  restore - Restore from backup directory"
        exit 1
        ;;
esac
