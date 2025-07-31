#!/bin/bash

# Kafka Web Tool - Quick Deployment Script
# Uses pre-built images from Docker registry for fast deployment
# Perfect for: Testing, demos, production deployments with stable images

set -e

# Default Configuration (can be overridden by environment variables)
NAMESPACE="${NAMESPACE:-kafka-tool}"
APP_NAME="${APP_NAME:-kafka-web-app-v2}"
BACKEND_IMAGE="${BACKEND_IMAGE:-kafka-web-app-v2}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-kafka-web-app-frontend}"
REGISTRY="${REGISTRY:-rmqk8}"
HOSTNAME="${HOSTNAME:-your-hostname.com}"

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
    echo -e "${BLUE}║                    🚀 Kafka Web Tool                        ║${NC}"
    echo -e "${BLUE}║                   Quick Deployment                          ║${NC}"
    echo -e "${BLUE}║                                                              ║${NC}"
    echo -e "${BLUE}║  Uses pre-built images for fast deployment                  ║${NC}"
    echo -e "${BLUE}║  Perfect for: Testing, demos, production                    ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Configuration setup function
setup_configuration() {
    echo ""
    log_step "Configuration Setup"
    echo "=============================="
    echo ""
    
    # Check if configuration is already set via environment variables
    if [ "$REGISTRY" != "rmqk8" ] && [ "$HOSTNAME" != "your-hostname.com" ]; then
        log_info "Using environment variables configuration:"
        echo "  Registry: $REGISTRY"
        echo "  Hostname: $HOSTNAME"
        echo "  Namespace: $NAMESPACE"
        echo ""
        read -p "Continue with this configuration? (y/n): " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            return 0
        fi
    fi
    
    echo "Please provide your deployment configuration:"
    echo ""
    
    # Docker Registry
    echo "📦 Docker Registry Configuration"
    echo "Examples: your-dockerhub-username, gcr.io/your-project, your-registry.com"
    echo "Default: rmqk8 (official Kafka Web Tool images)"
    read -p "Enter your Docker registry [$REGISTRY]: " input_registry
    if [ ! -z "$input_registry" ]; then
        REGISTRY="$input_registry"
    fi
    
    # Hostname
    echo ""
    echo "🌐 Application Hostname"
    echo "Example: kafka-tool.your-domain.com"
    read -p "Enter your application hostname: " input_hostname
    if [ ! -z "$input_hostname" ]; then
        HOSTNAME="$input_hostname"
    fi
    
    # Namespace
    echo ""
    echo "🏷️  Kubernetes Namespace"
    echo "Default: kafka-tool"
    read -p "Enter Kubernetes namespace [$NAMESPACE]: " input_namespace
    if [ ! -z "$input_namespace" ]; then
        NAMESPACE="$input_namespace"
    fi
    
    echo ""
    log_info "Configuration summary:"
    echo "  Registry: $REGISTRY"
    echo "  Backend Image: $REGISTRY/$BACKEND_IMAGE:latest"
    echo "  Frontend Image: $REGISTRY/$FRONTEND_IMAGE:latest"
    echo "  Hostname: $HOSTNAME"
    echo "  Namespace: $NAMESPACE"
    echo ""
    
    # Validation
    if [ "$HOSTNAME" = "your-hostname.com" ]; then
        log_error "Please provide a valid hostname"
        exit 1
    fi
}

# Check prerequisites
check_prerequisites() {
    log_step "Checking Prerequisites"
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if we can connect to Kubernetes cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    # Check if images exist in registry
    log_info "Verifying images exist in registry..."
    if ! docker pull $REGISTRY/$BACKEND_IMAGE:latest &> /dev/null; then
        log_warning "Cannot verify backend image: $REGISTRY/$BACKEND_IMAGE:latest"
        echo "This might be normal if the registry requires authentication."
    fi
    
    if ! docker pull $REGISTRY/$FRONTEND_IMAGE:latest &> /dev/null; then
        log_warning "Cannot verify frontend image: $REGISTRY/$FRONTEND_IMAGE:latest"
        echo "This might be normal if the registry requires authentication."
    fi
    
    log_success "Prerequisites check completed"
}

# Create namespace
create_namespace() {
    log_step "Creating Namespace"
    
    if kubectl get namespace $NAMESPACE &> /dev/null; then
        log_warning "Namespace $NAMESPACE already exists"
    else
        kubectl create namespace $NAMESPACE
        log_success "Namespace $NAMESPACE created"
    fi
}

# Deploy infrastructure
deploy_infrastructure() {
    log_step "Deploying Infrastructure"
    
    log_info "Deploying ConfigMap..."
    sed -e "s/namespace: kafka-tool/namespace: $NAMESPACE/g" \
        -e "s/kafkawebtool.marsem.org/$HOSTNAME/g" \
        k8s/configmap.yaml | kubectl apply -f -
    
    log_info "Deploying Secrets..."
    sed "s/namespace: kafka-tool/namespace: $NAMESPACE/g" k8s/secret.yaml | kubectl apply -f -
    
    log_info "Deploying PostgreSQL..."
    sed "s/namespace: kafka-tool/namespace: $NAMESPACE/g" k8s/postgres.yaml | kubectl apply -f -
    
    log_info "Deploying Redis..."
    sed "s/namespace: kafka-tool/namespace: $NAMESPACE/g" k8s/redis.yaml | kubectl apply -f -
    
    log_success "Infrastructure deployed"
}

# Wait for infrastructure
wait_for_infrastructure() {
    log_step "Waiting for Infrastructure"
    
    log_info "Waiting for PostgreSQL..."
    kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=300s
    
    log_info "Waiting for Redis..."
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=120s
    
    log_success "Infrastructure is ready"
}

# Deploy application
deploy_application() {
    log_step "Deploying Application"
    
    log_info "Deploying backend and frontend..."
    sed "s/namespace: kafka-tool/namespace: $NAMESPACE/g" k8s/deployment.yaml | kubectl apply -f -
    
    log_success "Application deployed"
}

# Deploy ingress
deploy_ingress() {
    log_step "Deploying Ingress"
    
    sed -e "s/namespace: kafka-tool/namespace: $NAMESPACE/g" \
        -e "s/kafkawebtool.marsem.org/$HOSTNAME/g" \
        k8s/ingress.yaml | kubectl apply -f -
    
    log_success "Ingress deployed"
}

# Wait for deployment
wait_for_deployment() {
    log_step "Waiting for Deployment"
    
    log_info "Waiting for backend deployment..."
    kubectl rollout status deployment/kafka-web-app-backend -n $NAMESPACE --timeout=300s
    
    log_info "Waiting for frontend deployment..."
    kubectl rollout status deployment/kafka-web-app-frontend -n $NAMESPACE --timeout=180s
    
    log_success "All deployments are ready"
}

# Check deployment status
check_deployment() {
    log_step "Deployment Status"
    
    echo ""
    echo "=== Pod Status ==="
    kubectl get pods -n $NAMESPACE
    
    echo ""
    echo "=== Service Status ==="
    kubectl get services -n $NAMESPACE
    
    echo ""
    echo "=== Ingress Status ==="
    kubectl get ingress -n $NAMESPACE
    
    echo ""
    echo "=== Application URLs ==="
    echo "Frontend: https://$HOSTNAME"
    echo "Backend API: https://$HOSTNAME/api/v1"
    echo "Health Check: https://$HOSTNAME/api/v1/actuator/health"
    echo "API Documentation: https://$HOSTNAME/api/v1/swagger-ui.html"

    echo ""
    echo "=== Default Login Credentials ==="
    echo "Username: admin"
    echo "Password: admin123"
    echo ""
    echo "⚠️  Change these credentials in production!"

    echo ""
    echo "=== Features Available ==="
    echo "✅ Message Browsing & Consuming"
    echo "✅ Message Production"
    echo "✅ Edit Messages (sends updated version)"
    echo "✅ Delete Messages (sends tombstone)"
    echo "✅ Topic Management"
    echo "✅ Connection Management"
}

# Main deployment function
main() {
    show_banner
    setup_configuration
    
    log_info "Starting quick deployment of $APP_NAME to $NAMESPACE namespace..."
    echo ""
    
    check_prerequisites
    create_namespace
    deploy_infrastructure
    wait_for_infrastructure
    deploy_application
    deploy_ingress
    wait_for_deployment
    check_deployment
    
    echo ""
    log_success "🎉 Quick Deployment Completed Successfully!"
    echo ""
    echo "🌐 Access your application at: https://$HOSTNAME"
    echo "🔐 Login with: admin / admin123"
    echo "📚 Check the documentation for usage instructions"
    echo ""
}

# Parse command line arguments
case "${1:-deploy}" in
    "deploy")
        main
        ;;
    "status")
        setup_configuration
        check_deployment
        ;;
    "clean")
        setup_configuration
        log_info "Cleaning up deployment..."
        kubectl delete namespace $NAMESPACE --ignore-not-found=true
        log_success "Cleanup completed"
        ;;
    *)
        echo "Usage: $0 {deploy|status|clean}"
        echo "  deploy - Quick deployment using latest images (default)"
        echo "  status - Check deployment status"
        echo "  clean  - Remove the deployment"
        echo ""
        echo "Environment Variables:"
        echo "  REGISTRY  - Docker registry (default: rmqk8)"
        echo "  HOSTNAME  - Application hostname"
        echo "  NAMESPACE - Kubernetes namespace (default: kafka-tool)"
        exit 1
        ;;
esac
