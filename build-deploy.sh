#!/bin/bash

# Kafka Web Tool - Build & Deploy Script
# Builds new images from source code and deploys them
# Perfect for: Development, testing changes, creating new releases

set -e

# Default Configuration (can be overridden by environment variables)
NAMESPACE="${NAMESPACE:-kafka-tool}"
APP_NAME="${APP_NAME:-kafka-web-app-v2}"
BACKEND_IMAGE="${BACKEND_IMAGE:-kafka-web-app-v2}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-kafka-web-app-frontend}"
REGISTRY="${REGISTRY:-your-registry}"
HOSTNAME="${HOSTNAME:-your-hostname.com}"
BUILD_TAG="${BUILD_TAG:-latest}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
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

log_build() {
    echo -e "${CYAN}[BUILD]${NC} $1"
}

# Banner
show_banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                    🔨 Kafka Web Tool                        ║${NC}"
    echo -e "${CYAN}║                  Build & Deploy                             ║${NC}"
    echo -e "${CYAN}║                                                              ║${NC}"
    echo -e "${CYAN}║  Builds new images from source and deploys                  ║${NC}"
    echo -e "${CYAN}║  Perfect for: Development, testing changes                  ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Configuration setup function
setup_configuration() {
    echo ""
    log_step "Configuration Setup"
    echo "=============================="
    echo ""
    
    # Check if configuration is already set via environment variables
    if [ "$REGISTRY" != "your-registry" ] && [ "$HOSTNAME" != "your-hostname.com" ]; then
        log_info "Using environment variables configuration:"
        echo "  Registry: $REGISTRY"
        echo "  Hostname: $HOSTNAME"
        echo "  Namespace: $NAMESPACE"
        echo "  Build Tag: $BUILD_TAG"
        echo ""
        read -p "Continue with this configuration? (y/n): " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            return 0
        fi
    fi
    
    echo "Please provide your build and deployment configuration:"
    echo ""
    
    # Docker Registry
    echo "📦 Docker Registry Configuration"
    echo "Examples: your-dockerhub-username, gcr.io/your-project, your-registry.com"
    read -p "Enter your Docker registry: " input_registry
    if [ ! -z "$input_registry" ]; then
        REGISTRY="$input_registry"
    fi
    
    # Build Tag
    echo ""
    echo "🏷️  Build Tag"
    echo "Examples: latest, v1.0.0, dev-$(date +%Y%m%d)"
    read -p "Enter build tag [$BUILD_TAG]: " input_tag
    if [ ! -z "$input_tag" ]; then
        BUILD_TAG="$input_tag"
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
    echo "  Backend Image: $REGISTRY/$BACKEND_IMAGE:$BUILD_TAG"
    echo "  Frontend Image: $REGISTRY/$FRONTEND_IMAGE:$BUILD_TAG"
    echo "  Hostname: $HOSTNAME"
    echo "  Namespace: $NAMESPACE"
    echo ""
    
    # Validation
    if [ "$REGISTRY" = "your-registry" ] || [ "$HOSTNAME" = "your-hostname.com" ]; then
        log_error "Please provide valid registry and hostname values"
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
    
    # Check if docker is installed
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check if maven is installed
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    
    # Check if npm is installed
    if ! command -v npm &> /dev/null; then
        log_error "npm is not installed. Please install Node.js and npm first."
        exit 1
    fi
    
    # Check if we can connect to Kubernetes cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info &> /dev/null; then
        log_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Build application
build_application() {
    log_step "Building Application"
    
    # Get current git commit for tagging
    GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    FULL_TAG="${BUILD_TAG}-${TIMESTAMP}-${GIT_COMMIT}"
    
    log_info "Build information:"
    echo "  Git Commit: $GIT_COMMIT"
    echo "  Timestamp: $TIMESTAMP"
    echo "  Full Tag: $FULL_TAG"
    echo ""
    
    # Build backend
    log_build "Building backend..."
    cd backend
    log_info "Compiling Java application with Maven..."
    mvn clean package -DskipTests
    
    log_info "Building backend Docker image..."
    docker buildx build --platform linux/amd64 \
        -t $REGISTRY/$BACKEND_IMAGE:$BUILD_TAG \
        -t $REGISTRY/$BACKEND_IMAGE:$FULL_TAG \
        --push \
        .
    cd ..
    
    # Build frontend
    log_build "Building frontend..."
    cd frontend
    log_info "Installing npm dependencies..."
    npm install
    
    log_info "Building React application..."
    # Use Docker build to avoid local Node.js compatibility issues
    docker buildx build --platform linux/amd64 \
        -t $REGISTRY/$FRONTEND_IMAGE:$BUILD_TAG \
        -t $REGISTRY/$FRONTEND_IMAGE:$FULL_TAG \
        --push \
        .
    cd ..
    
    log_success "Application built successfully"
    echo "Backend image: $REGISTRY/$BACKEND_IMAGE:$BUILD_TAG"
    echo "Frontend image: $REGISTRY/$FRONTEND_IMAGE:$BUILD_TAG"
}

# Push to registry
push_to_registry() {
    log_step "Verifying Images in Registry"

    log_info "Images were pushed during build process..."
    log_info "Backend image: $REGISTRY/$BACKEND_IMAGE:$BUILD_TAG"
    log_info "Frontend image: $REGISTRY/$FRONTEND_IMAGE:$BUILD_TAG"

    # Verify images exist in registry
    log_info "Verifying backend image..."
    if docker manifest inspect $REGISTRY/$BACKEND_IMAGE:$BUILD_TAG &> /dev/null; then
        log_success "Backend image verified in registry"
    else
        log_warning "Cannot verify backend image (may require authentication)"
    fi

    log_info "Verifying frontend image..."
    if docker manifest inspect $REGISTRY/$FRONTEND_IMAGE:$BUILD_TAG &> /dev/null; then
        log_success "Frontend image verified in registry"
    else
        log_warning "Cannot verify frontend image (may require authentication)"
    fi

    log_success "Images available in registry"
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
        -e "s/ddl-auto: validate/ddl-auto: update/g" \
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
    
    # Update deployment with new image tags
    log_info "Deploying backend and frontend with new images..."
    sed -e "s/namespace: kafka-tool/namespace: $NAMESPACE/g" \
        -e "s/:latest/:$BUILD_TAG/g" \
        k8s/deployment.yaml | kubectl apply -f -
    
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
    echo "=== Build Information ==="
    echo "Backend Image: $REGISTRY/$BACKEND_IMAGE:$BUILD_TAG"
    echo "Frontend Image: $REGISTRY/$FRONTEND_IMAGE:$BUILD_TAG"
    echo "Git Commit: $(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
    echo "Build Time: $(date)"
}

# Main deployment function
main() {
    show_banner
    setup_configuration
    
    log_info "Starting build and deployment of $APP_NAME to $NAMESPACE namespace..."
    echo ""
    
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
    
    echo ""
    log_success "🎉 Build & Deployment Completed Successfully!"
    echo ""
    echo "🌐 Access your application at: https://$HOSTNAME"
    echo "🔐 Login with: admin / admin123"
    echo "📚 Check the documentation for usage instructions"
    echo ""
}

# Parse command line arguments
case "${1:-deploy}" in
    "build")
        setup_configuration
        check_prerequisites
        build_application
        push_to_registry
        ;;
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
        echo "Usage: $0 {build|deploy|status|clean}"
        echo "  build  - Build and push images only"
        echo "  deploy - Full build and deployment (default)"
        echo "  status - Check deployment status"
        echo "  clean  - Remove the deployment"
        echo ""
        echo "Environment Variables:"
        echo "  REGISTRY   - Docker registry"
        echo "  HOSTNAME   - Application hostname"
        echo "  NAMESPACE  - Kubernetes namespace (default: kafka-tool)"
        echo "  BUILD_TAG  - Docker image tag (default: latest)"
        exit 1
        ;;
esac
