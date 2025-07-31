#!/bin/bash

# Kafka Web App v2 Deployment Script
# This script builds and deploys the application to Kubernetes

set -e

# Configuration
NAMESPACE="kafka-tool"
APP_NAME="kafka-web-app-v2"
BACKEND_IMAGE="kafka-web-app-v2:latest"
REGISTRY="your-registry.com"  # Update with your container registry

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if docker is installed
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check if we can connect to Kubernetes cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Build the application
build_application() {
    log_info "Building the application..."
    
    cd backend
    
    # Build with Maven
    log_info "Building with Maven..."
    mvn clean package -DskipTests
    
    # Build Docker image
    log_info "Building Docker image..."
    docker build -t $BACKEND_IMAGE .
    
    # Tag for registry if specified
    if [ "$REGISTRY" != "your-registry.com" ]; then
        docker tag $BACKEND_IMAGE $REGISTRY/$BACKEND_IMAGE
        log_info "Tagged image for registry: $REGISTRY/$BACKEND_IMAGE"
    fi
    
    cd ..
    log_success "Application built successfully"
}

# Push to registry
push_to_registry() {
    if [ "$REGISTRY" != "your-registry.com" ]; then
        log_info "Pushing image to registry..."
        docker push $REGISTRY/$BACKEND_IMAGE
        log_success "Image pushed to registry"
    else
        log_warning "No registry configured, skipping push"
    fi
}

# Create namespace
create_namespace() {
    log_info "Creating namespace..."
    
    if kubectl get namespace $NAMESPACE &> /dev/null; then
        log_warning "Namespace $NAMESPACE already exists"
    else
        kubectl apply -f k8s/namespace.yaml
        log_success "Namespace $NAMESPACE created"
    fi
}

# Deploy infrastructure
deploy_infrastructure() {
    log_info "Deploying infrastructure components..."
    
    # Apply ConfigMaps and Secrets
    kubectl apply -f k8s/configmap.yaml
    kubectl apply -f k8s/secret.yaml
    kubectl apply -f k8s/nginx-config.yaml
    
    # Deploy PostgreSQL
    log_info "Deploying PostgreSQL..."
    kubectl apply -f k8s/postgres.yaml
    
    # Deploy Redis
    log_info "Deploying Redis..."
    kubectl apply -f k8s/redis.yaml
    
    log_success "Infrastructure components deployed"
}

# Wait for infrastructure
wait_for_infrastructure() {
    log_info "Waiting for infrastructure to be ready..."
    
    # Wait for PostgreSQL
    log_info "Waiting for PostgreSQL..."
    kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=300s
    
    # Wait for Redis
    log_info "Waiting for Redis..."
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=300s
    
    log_success "Infrastructure is ready"
}

# Deploy application
deploy_application() {
    log_info "Deploying application..."
    
    # Update image in deployment if using registry
    if [ "$REGISTRY" != "your-registry.com" ]; then
        sed -i.bak "s|kafka-web-app-v2:latest|$REGISTRY/$BACKEND_IMAGE|g" k8s/deployment.yaml
    fi
    
    # Apply deployment
    kubectl apply -f k8s/deployment.yaml
    
    # Restore original deployment file if modified
    if [ -f k8s/deployment.yaml.bak ]; then
        mv k8s/deployment.yaml.bak k8s/deployment.yaml
    fi
    
    log_success "Application deployed"
}

# Deploy ingress
deploy_ingress() {
    log_info "Deploying ingress..."
    
    # Check if cert-manager is installed
    if kubectl get crd certificates.cert-manager.io &> /dev/null; then
        kubectl apply -f k8s/ingress.yaml
        log_success "Ingress deployed with SSL"
    else
        log_warning "cert-manager not found, deploying ingress without SSL"
        # Create a version without cert-manager annotations
        sed '/cert-manager.io/d' k8s/ingress.yaml | kubectl apply -f -
    fi
}

# Wait for deployment
wait_for_deployment() {
    log_info "Waiting for application to be ready..."
    
    # Wait for backend deployment
    kubectl wait --for=condition=available deployment/kafka-web-app-backend -n $NAMESPACE --timeout=600s
    
    # Wait for frontend deployment
    kubectl wait --for=condition=available deployment/kafka-web-app-frontend -n $NAMESPACE --timeout=300s
    
    log_success "Application is ready"
}

# Check deployment status
check_deployment() {
    log_info "Checking deployment status..."
    
    echo ""
    echo "=== Namespace ==="
    kubectl get namespace $NAMESPACE
    
    echo ""
    echo "=== Pods ==="
    kubectl get pods -n $NAMESPACE
    
    echo ""
    echo "=== Services ==="
    kubectl get services -n $NAMESPACE
    
    echo ""
    echo "=== Ingress ==="
    kubectl get ingress -n $NAMESPACE
    
    echo ""
    echo "=== Application URLs ==="
    echo "Frontend: https://kafkatool.marsem.org"
    echo "Backend API: https://kafkatool.marsem.org/api/v1"
    echo "Health Check: https://kafkatool.marsem.org/api/v1/health"
    echo "API Documentation: https://kafkatool.marsem.org/api/v1/swagger-ui.html"
    
    log_success "Deployment completed successfully!"
}

# Main deployment function
main() {
    log_info "Starting deployment of $APP_NAME to $NAMESPACE namespace..."
    
    check_prerequisites
    build_application
    push_to_registry
    create_namespace
    deploy_infrastructure
    wait_for_infrastructure
    deploy_application
    deploy_ingress
    wait_for_deployment
    check_deployment
    
    log_success "Deployment completed! 🚀"
    echo ""
    echo "Access your application at: https://kafkatool.marsem.org"
}

# Parse command line arguments
case "${1:-deploy}" in
    "build")
        check_prerequisites
        build_application
        ;;
    "deploy")
        main
        ;;
    "status")
        check_deployment
        ;;
    "clean")
        log_info "Cleaning up deployment..."
        kubectl delete namespace $NAMESPACE --ignore-not-found=true
        log_success "Cleanup completed"
        ;;
    *)
        echo "Usage: $0 {build|deploy|status|clean}"
        echo "  build  - Build the application only"
        echo "  deploy - Full deployment (default)"
        echo "  status - Check deployment status"
        echo "  clean  - Remove the deployment"
        exit 1
        ;;
esac
